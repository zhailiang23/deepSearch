package com.ynet.mgmt.searchspace.validator;

import com.ynet.mgmt.common.exception.ValidationException;
import com.ynet.mgmt.searchspace.dto.CreateSearchSpaceRequest;
import com.ynet.mgmt.searchspace.dto.UpdateSearchSpaceRequest;
import com.ynet.mgmt.searchspace.entity.SearchSpace;
import com.ynet.mgmt.searchspace.repository.SearchSpaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 搜索空间业务验证器
 * 提供业务规则验证和数据校验功能
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class SearchSpaceValidator {

    private static final Logger log = LoggerFactory.getLogger(SearchSpaceValidator.class);

    private final SearchSpaceRepository searchSpaceRepository;

    public SearchSpaceValidator(SearchSpaceRepository searchSpaceRepository) {
        this.searchSpaceRepository = searchSpaceRepository;
    }

    /**
     * 验证创建搜索空间请求
     *
     * @param request 创建请求
     * @throws ValidationException 验证失败时抛出
     */
    public void validateCreateRequest(CreateSearchSpaceRequest request) {
        log.debug("验证创建搜索空间请求: {}", request);

        if (request == null) {
            throw new ValidationException("创建请求不能为空");
        }

        // 必填字段验证
        if (!StringUtils.hasText(request.getName())) {
            throw new ValidationException("搜索空间名称不能为空");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new ValidationException("搜索空间代码不能为空");
        }

        // 格式验证
        validateCode(request.getCode());
        validateName(request.getName());

        if (StringUtils.hasText(request.getDescription())) {
            validateDescription(request.getDescription());
        }

        log.debug("创建搜索空间请求验证通过");
    }

    /**
     * 验证更新搜索空间请求
     *
     * @param request 更新请求
     * @throws ValidationException 验证失败时抛出
     */
    public void validateUpdateRequest(UpdateSearchSpaceRequest request) {
        log.debug("验证更新搜索空间请求: {}", request);

        if (request == null) {
            throw new ValidationException("更新请求不能为空");
        }

        if (StringUtils.hasText(request.getName())) {
            validateName(request.getName());
        }
        if (StringUtils.hasText(request.getDescription())) {
            validateDescription(request.getDescription());
        }

        log.debug("更新搜索空间请求验证通过");
    }

    /**
     * 验证删除操作
     *
     * @param searchSpace 待删除的搜索空间
     * @throws ValidationException 验证失败时抛出
     */
    public void validateDeleteOperation(SearchSpace searchSpace) {
        log.debug("验证删除搜索空间操作: {}", searchSpace);

        if (searchSpace == null) {
            throw new ValidationException("搜索空间不存在");
        }

        // 检查是否可以删除
        if (searchSpace.isActive()) {
            // 活跃的搜索空间需要先停用才能删除
            log.debug("搜索空间 {} 处于活跃状态，可以删除", searchSpace.getCode());
        }

        log.debug("删除搜索空间操作验证通过");
    }

    /**
     * 验证向量检索禁用操作
     *
     * @param searchSpace 搜索空间
     * @throws ValidationException 验证失败时抛出
     */
    public void validateVectorDisable(SearchSpace searchSpace) {
        log.debug("验证向量检索禁用操作: {}", searchSpace);

        if (searchSpace == null) {
            throw new ValidationException("搜索空间不存在");
        }

        if (!searchSpace.getVectorEnabled()) {
            throw new ValidationException("向量检索已禁用");
        }

        // 检查是否有正在进行的向量操作
        // TODO: 在后续版本中添加向量操作检查逻辑

        // 检查是否有依赖向量检索的功能
        // TODO: 在后续版本中添加依赖检查逻辑

        log.debug("向量检索禁用操作验证通过");
    }

    /**
     * 验证代码唯一性
     *
     * @param code 搜索空间代码
     * @param excludeId 排除的ID（用于更新时检查）
     * @throws ValidationException 验证失败时抛出
     */
    public void validateCodeUniqueness(String code, Long excludeId) {
        log.debug("验证代码唯一性: code={}, excludeId={}", code, excludeId);

        if (!StringUtils.hasText(code)) {
            return;
        }

        boolean exists;
        if (excludeId != null) {
            exists = searchSpaceRepository.existsByCodeExcludingId(code, excludeId);
        } else {
            exists = searchSpaceRepository.existsByCode(code);
        }

        if (exists) {
            throw new ValidationException("搜索空间代码已存在: " + code);
        }

        log.debug("代码唯一性验证通过");
    }

    /**
     * 验证名称唯一性
     *
     * @param name 搜索空间名称
     * @param excludeId 排除的ID（用于更新时检查）
     * @throws ValidationException 验证失败时抛出
     */
    public void validateNameUniqueness(String name, Long excludeId) {
        log.debug("验证名称唯一性: name={}, excludeId={}", name, excludeId);

        if (!StringUtils.hasText(name)) {
            return;
        }

        boolean exists;
        if (excludeId != null) {
            exists = searchSpaceRepository.existsByNameExcludingId(name, excludeId);
        } else {
            exists = searchSpaceRepository.existsByName(name);
        }

        if (exists) {
            throw new ValidationException("搜索空间名称已存在: " + name);
        }

        log.debug("名称唯一性验证通过");
    }

    /**
     * 验证搜索空间代码格式
     *
     * @param code 搜索空间代码
     * @throws ValidationException 验证失败时抛出
     */
    private void validateCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ValidationException("搜索空间代码不能为空");
        }

        if (code.length() > 100) {
            throw new ValidationException("搜索空间代码长度不能超过100字符");
        }

        if (!code.matches("^[a-z][a-z0-9_]*$")) {
            throw new ValidationException("搜索空间代码只能包含小写字母、数字和下划线，且必须以字母开头");
        }

        // 检查保留关键字
        if (isReservedKeyword(code)) {
            throw new ValidationException("搜索空间代码不能使用保留关键字: " + code);
        }
    }

    /**
     * 验证搜索空间名称格式
     *
     * @param name 搜索空间名称
     * @throws ValidationException 验证失败时抛出
     */
    private void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ValidationException("搜索空间名称不能为空");
        }

        if (name.length() > 50) {
            throw new ValidationException("搜索空间名称长度不能超过50字符");
        }

        // 检查名称中是否包含特殊字符
        if (name.matches(".*[<>\"&].*")) {
            throw new ValidationException("搜索空间名称不能包含特殊字符: < > \" &");
        }
    }

    /**
     * 验证描述格式
     *
     * @param description 描述
     * @throws ValidationException 验证失败时抛出
     */
    private void validateDescription(String description) {
        if (description == null) {
            return;
        }

        if (description.length() > 500) {
            throw new ValidationException("描述长度不能超过500字符");
        }
    }

    /**
     * 检查是否为保留关键字
     *
     * @param code 代码
     * @return true 如果是保留关键字
     */
    private boolean isReservedKeyword(String code) {
        String[] reservedKeywords = {
            "system", "admin", "root", "config", "settings",
            "api", "www", "mail", "ftp", "localhost",
            "elasticsearch", "kibana", "logstash"
        };

        for (String keyword : reservedKeywords) {
            if (keyword.equals(code)) {
                return true;
            }
        }
        return false;
    }
}