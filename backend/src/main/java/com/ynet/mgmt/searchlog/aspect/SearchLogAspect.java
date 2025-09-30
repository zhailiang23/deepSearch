package com.ynet.mgmt.searchlog.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchdata.dto.SearchDataRequest;
import com.ynet.mgmt.searchdata.dto.SearchDataResponse;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.service.SearchLogService;
import com.ynet.mgmt.searchspace.service.SearchSpaceService;
import com.ynet.mgmt.searchspace.dto.SearchSpaceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CompletableFuture;

/**
 * 搜索日志记录切面
 * 自动拦截ElasticsearchDataController.searchData方法的调用，
 * 异步记录完整的搜索请求、响应和性能数据
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(value = "search.log.enabled", havingValue = "true", matchIfMissing = true)
public class SearchLogAspect {

    private final SearchLogService searchLogService;
    private final SearchSpaceService searchSpaceService;
    private final ObjectMapper objectMapper;

    /**
     * 拦截searchData方法执行
     */
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) && " +
            "execution(* com.ynet.mgmt.searchdata.controller.ElasticsearchDataController.searchData(..))")
    public Object logSearchData(ProceedingJoinPoint joinPoint) throws Throwable {
        // 记录搜索开始时间
        long startTime = System.currentTimeMillis();

        // 获取请求参数
        SearchDataRequest request = extractSearchRequest(joinPoint);
        if (request == null) {
            log.warn("无法提取搜索请求参数，跳过日志记录");
            return joinPoint.proceed();
        }

        // 获取用户信息
        String userId = getCurrentUserId();
        String userIp = getCurrentUserIp();
        String userAgent = getCurrentUserAgent();
        String sessionId = getCurrentSessionId();

        try {
            // 执行原始方法
            Object result = joinPoint.proceed();

            // 同步记录成功日志并获取搜索日志ID
            Long searchLogId = recordSearchLogSync(request, result, userId, userIp, userAgent, sessionId, startTime, null);

            // 将搜索日志ID设置到响应中
            if (result instanceof ResponseEntity) {
                setSearchLogIdInResponse((ResponseEntity<?>) result, searchLogId);
            }

            return result;

        } catch (Exception e) {
            // 异步记录失败日志（失败情况下不需要返回ID）
            recordSearchLogAsync(request, null, userId, userIp, userAgent, sessionId, startTime, e);
            throw e;
        }
    }

    /**
     * 同步记录搜索日志并返回日志ID
     */
    public Long recordSearchLogSync(
            SearchDataRequest request,
            Object response,
            String userId,
            String userIp,
            String userAgent,
            String sessionId,
            long startTime,
            Exception exception) {

        try {
            SearchLog searchLog = buildSearchLog(request, response, userId, userIp, userAgent, sessionId, startTime, exception);

            // 同步保存搜索日志
            SearchLog savedLog = searchLogService.saveSearchLog(searchLog);

            log.debug("搜索日志记录成功: userId={}, query={}, responseTime={}ms, status={}, logId={}",
                    userId, request.getQuery(), searchLog.getTotalTimeMs(), searchLog.getStatus(), savedLog.getId());

            return savedLog.getId();

        } catch (Exception e) {
            log.error("同步记录搜索日志失败", e);
            return null;
        }
    }

    /**
     * 异步记录搜索日志
     */
    @Async("searchLogExecutor")
    public CompletableFuture<Void> recordSearchLogAsync(
            SearchDataRequest request,
            Object response,
            String userId,
            String userIp,
            String userAgent,
            String sessionId,
            long startTime,
            Exception exception) {

        try {
            SearchLog searchLog = buildSearchLog(request, response, userId, userIp, userAgent, sessionId, startTime, exception);

            // 保存搜索日志
            searchLogService.saveSearchLog(searchLog);

            log.debug("搜索日志记录成功: userId={}, query={}, responseTime={}ms, status={}",
                    userId, request.getQuery(), searchLog.getTotalTimeMs(), searchLog.getStatus());

        } catch (Exception e) {
            log.error("异步记录搜索日志失败", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 在响应中设置搜索日志ID
     */
    private void setSearchLogIdInResponse(ResponseEntity<?> responseEntity, Long searchLogId) {
        if (searchLogId == null) {
            return;
        }

        try {
            Object body = responseEntity.getBody();
            if (body instanceof ApiResponse) {
                ApiResponse<?> apiResponse = (ApiResponse<?>) body;
                if (apiResponse.isSuccess() && apiResponse.getData() instanceof SearchDataResponse) {
                    SearchDataResponse searchResponse = (SearchDataResponse) apiResponse.getData();
                    searchResponse.setSearchLogId(searchLogId);
                    log.debug("搜索日志ID已设置到响应中: {}", searchLogId);
                }
            }
        } catch (Exception e) {
            log.warn("设置搜索日志ID到响应失败: {}", searchLogId, e);
        }
    }

    /**
     * 构建搜索日志对象
     */
    private SearchLog buildSearchLog(
            SearchDataRequest request,
            Object response,
            String userId,
            String userIp,
            String userAgent,
            String sessionId,
            long startTime,
            Exception exception) {

        SearchLog searchLog = new SearchLog();

            // 基本信息
            if (userId != null && !userId.equals("anonymous")) {
                try {
                    searchLog.setUserId(Long.valueOf(userId));
                } catch (NumberFormatException e) {
                    log.debug("用户ID格式错误，使用默认值: {}", userId);
                    searchLog.setUserId(null);
                }
            }
            searchLog.setIpAddress(userIp);

            // 处理搜索空间ID - 支持单空间和多空间搜索
            String searchSpaceIdStr = request.getSearchSpaceId();
            if (searchSpaceIdStr == null || searchSpaceIdStr.trim().isEmpty()) {
                // 如果是多空间搜索，使用第一个空间ID
                if (request.getSearchSpaceIds() != null && !request.getSearchSpaceIds().isEmpty()) {
                    searchSpaceIdStr = request.getSearchSpaceIds().get(0);
                }
            }

            // 设置搜索空间ID
            if (searchSpaceIdStr != null && !searchSpaceIdStr.trim().isEmpty()) {
                try {
                    searchLog.setSearchSpaceId(Long.valueOf(searchSpaceIdStr));
                } catch (NumberFormatException e) {
                    log.warn("搜索空间ID格式错误: {}", searchSpaceIdStr, e);
                    searchLog.setSearchSpaceId(null);
                }
            }

            searchLog.setSearchQuery(request.getQuery());
            searchLog.setUserAgent(userAgent);
            searchLog.setSessionId(sessionId);

            // 获取并设置搜索空间信息
            if (searchSpaceIdStr != null && !searchSpaceIdStr.trim().isEmpty()) {
                try {
                    SearchSpaceDTO searchSpace = searchSpaceService.getSearchSpace(Long.valueOf(searchSpaceIdStr));
                    if (searchSpace != null) {
                        searchLog.setSearchSpaceCode(searchSpace.getCode());
                        searchLog.setSearchSpaceName(searchSpace.getName());
                    }
                } catch (Exception e) {
                    log.warn("获取搜索空间信息失败，searchSpaceId: {}", searchSpaceIdStr, e);
                }
            }

            // 设置请求参数
            searchLog.setPageNumber(request.getPage());
            searchLog.setPageSize(request.getSize());
            searchLog.setSearchMode(request.getPinyinMode());
            searchLog.setEnablePinyin(request.getEnablePinyinSearch());

            // 序列化请求参数为 JSON
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                searchLog.setRequestParams(requestJson);
            } catch (Exception e) {
                log.warn("序列化请求参数失败", e);
                searchLog.setRequestParams(null);
            }

            // 处理响应数据
            if (response != null && response instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
                if (responseEntity.getBody() instanceof ApiResponse) {
                    ApiResponse<?> apiResponse = (ApiResponse<?>) responseEntity.getBody();

                    // 序列化响应数据为 JSON
                    try {
                        String responseJson = objectMapper.writeValueAsString(apiResponse);
                        searchLog.setResponseData(responseJson);
                    } catch (Exception e) {
                        log.warn("序列化响应数据失败", e);
                        searchLog.setResponseData(null);
                    }

                    if (apiResponse.isSuccess() && apiResponse.getData() instanceof SearchDataResponse) {
                        SearchDataResponse searchResponse = (SearchDataResponse) apiResponse.getData();
                        searchLog.setTotalResults(searchResponse.getTotal());
                        searchLog.setReturnedResults(searchResponse.getData() != null ? searchResponse.getData().size() : 0);
                        searchLog.setStatus(SearchLogStatus.SUCCESS);
                    } else {
                        searchLog.setStatus(SearchLogStatus.ERROR);
                        searchLog.setErrorMessage("API响应失败: " + apiResponse.getMessage());
                    }
                }
            }

            // 处理异常情况
            if (exception != null) {
                searchLog.setStatus(SearchLogStatus.ERROR);
                searchLog.setErrorMessage(exception.getMessage());
                searchLog.setTotalResults(0L);
                searchLog.setReturnedResults(0);

                // 异常情况下也尝试序列化请求参数
                if (searchLog.getRequestParams() == null) {
                    try {
                        String requestJson = objectMapper.writeValueAsString(request);
                        searchLog.setRequestParams(requestJson);
                    } catch (Exception e) {
                        log.warn("异常情况下序列化请求参数失败", e);
                    }
                }
            }

            // 计算响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            searchLog.setTotalTimeMs(responseTime);

            // 设置其他字段
            if (searchLog.getStatus() == null) {
                searchLog.setStatus(SearchLogStatus.SUCCESS);
            }

        return searchLog;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 从切点参数中提取搜索请求
     */
    private SearchDataRequest extractSearchRequest(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof SearchDataRequest) {
                    return (SearchDataRequest) arg;
                }
            }
        }
        return null;
    }

    /**
     * 获取当前用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
                return authentication.getName();
            }
        } catch (Exception e) {
            log.debug("获取用户信息失败", e);
        }
        return "anonymous";
    }

    /**
     * 获取当前用户IP
     */
    private String getCurrentUserIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            // 检查代理头信息
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xForwardedFor)) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(xRealIp)) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            log.debug("获取用户IP失败", e);
            return "unknown";
        }
    }

    /**
     * 获取用户代理信息
     */
    private String getCurrentUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            log.debug("获取用户代理失败", e);
            return null;
        }
    }

    /**
     * 获取会话ID
     */
    private String getCurrentSessionId() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request.getSession(false) != null ? request.getSession().getId() : null;
        } catch (Exception e) {
            log.debug("获取会话ID失败", e);
            return null;
        }
    }
}