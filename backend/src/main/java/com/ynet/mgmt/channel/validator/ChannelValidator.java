package com.ynet.mgmt.channel.validator;

import com.ynet.mgmt.channel.dto.CreateChannelRequest;
import com.ynet.mgmt.channel.dto.UpdateChannelRequest;
import com.ynet.mgmt.channel.repository.ChannelRepository;
import com.ynet.mgmt.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 渠道业务验证器
 * 提供渠道管理的业务规则验证和数据校验功能
 *
 * @author system
 * @since 1.0.0
 */
@Component
public class ChannelValidator {

    private static final Logger log = LoggerFactory.getLogger(ChannelValidator.class);

    private final ChannelRepository channelRepository;

    // 验证规则常量
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{2,50}$");

    public ChannelValidator(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    /**
     * 验证创建渠道请求
     *
     * @param request 创建请求
     * @throws ValidationException 验证失败时抛出
     */
    public void validateCreateRequest(CreateChannelRequest request) {
        log.debug("验证创建渠道请求: {}", request);

        if (request == null) {
            throw new ValidationException("创建请求不能为空");
        }

        // 必填字段验证
        if (!StringUtils.hasText(request.getName())) {
            throw new ValidationException("渠道名称不能为空");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new ValidationException("渠道代码不能为空");
        }

        // 格式验证
        validateCode(request.getCode());
        validateName(request.getName());

        if (StringUtils.hasText(request.getDescription())) {
            validateDescription(request.getDescription());
        }

        log.debug("创建渠道请求验证通过");
    }

    /**
     * 验证更新渠道请求
     *
     * @param request 更新请求
     * @throws ValidationException 验证失败时抛出
     */
    public void validateUpdateRequest(UpdateChannelRequest request) {
        log.debug("验证更新渠道请求: {}", request);

        if (request == null) {
            throw new ValidationException("更新请求不能为空");
        }

        // 字段格式验证（仅验证非空字段）
        if (StringUtils.hasText(request.getName())) {
            validateName(request.getName());
        }
        if (StringUtils.hasText(request.getDescription())) {
            validateDescription(request.getDescription());
        }

        log.debug("更新渠道请求验证通过");
    }

    /**
     * 验证渠道代码唯一性
     *
     * @param code 渠道代码
     * @param excludeId 排除的渠道ID（更新时使用）
     * @throws ValidationException 验证失败时抛出
     */
    public void validateCodeUniqueness(String code, Long excludeId) {
        log.debug("验证渠道代码唯一性: code={}, excludeId={}", code, excludeId);

        boolean exists = excludeId != null ?
            channelRepository.existsByCodeAndIdNot(code, excludeId) :
            channelRepository.existsByCode(code);

        if (exists) {
            throw new ValidationException("渠道代码已存在: " + code);
        }
    }

    /**
     * 验证渠道名称唯一性
     *
     * @param name 渠道名称
     * @param excludeId 排除的渠道ID（更新时使用）
     * @throws ValidationException 验证失败时抛出
     */
    public void validateNameUniqueness(String name, Long excludeId) {
        log.debug("验证渠道名称唯一性: name={}, excludeId={}", name, excludeId);

        boolean exists = excludeId != null ?
            channelRepository.existsByNameAndIdNot(name, excludeId) :
            channelRepository.existsByName(name);

        if (exists) {
            throw new ValidationException("渠道名称已存在: " + name);
        }
    }

    // ========== 私有验证方法 ==========

    /**
     * 验证渠道代码格式
     */
    private void validateCode(String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ValidationException("渠道代码格式不正确，只能包含字母、数字和下划线，长度2-50字符");
        }
    }

    /**
     * 验证渠道名称
     */
    private void validateName(String name) {
        if (name.length() > 100) {
            throw new ValidationException("渠道名称长度不能超过100字符");
        }
    }

    /**
     * 验证渠道描述
     */
    private void validateDescription(String description) {
        if (description.length() > 500) {
            throw new ValidationException("渠道描述长度不能超过500字符");
        }
    }
}