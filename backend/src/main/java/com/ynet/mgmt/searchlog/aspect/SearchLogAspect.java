package com.ynet.mgmt.searchlog.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ynet.mgmt.common.dto.ApiResponse;
import com.ynet.mgmt.searchdata.dto.SearchDataRequest;
import com.ynet.mgmt.searchdata.dto.SearchDataResponse;
import com.ynet.mgmt.searchlog.entity.SearchLog;
import com.ynet.mgmt.searchlog.entity.SearchLogStatus;
import com.ynet.mgmt.searchlog.service.SearchLogService;
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
import java.time.LocalDateTime;
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

            // 异步记录成功日志
            recordSearchLogAsync(request, result, userId, userIp, userAgent, sessionId, startTime, null);

            return result;

        } catch (Exception e) {
            // 异步记录失败日志
            recordSearchLogAsync(request, null, userId, userIp, userAgent, sessionId, startTime, e);
            throw e;
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
            searchLog.setSearchSpaceId(Long.valueOf(request.getSearchSpaceId()));
            searchLog.setSearchQuery(request.getQuery());
            searchLog.setUserAgent(userAgent);
            searchLog.setSessionId(sessionId);

            // 设置请求参数
            searchLog.setPageNumber(request.getPage());
            searchLog.setPageSize(request.getSize());
            searchLog.setSearchMode(request.getPinyinMode());
            searchLog.setEnablePinyin(request.getEnablePinyinSearch());

            // 处理响应数据
            if (response != null && response instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) response;
                if (responseEntity.getBody() instanceof ApiResponse) {
                    ApiResponse<?> apiResponse = (ApiResponse<?>) responseEntity.getBody();

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
            }

            // 计算响应时间
            long responseTime = System.currentTimeMillis() - startTime;
            searchLog.setTotalTimeMs(responseTime);

            // 设置其他字段
            if (searchLog.getStatus() == null) {
                searchLog.setStatus(SearchLogStatus.SUCCESS);
            }

            // 保存搜索日志
            searchLogService.saveSearchLog(searchLog);

            log.debug("搜索日志记录成功: userId={}, query={}, responseTime={}ms, status={}",
                    userId, request.getQuery(), responseTime, searchLog.getStatus());

        } catch (Exception e) {
            log.error("异步记录搜索日志失败", e);
        }

        return CompletableFuture.completedFuture(null);
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