package com.ynet.mgmt.channel.validator;

import com.ynet.mgmt.channel.constants.ErrorCode;
import com.ynet.mgmt.channel.dto.CreateChannelRequest;
import com.ynet.mgmt.channel.dto.UpdateChannelRequest;
import com.ynet.mgmt.channel.entity.Channel;
import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.repository.ChannelRepository;
import com.ynet.mgmt.common.exception.BusinessException;
import com.ynet.mgmt.common.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
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
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s()]{0,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

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
        if (request.getType() == null) {
            throw new ValidationException("渠道类型不能为空");
        }

        // 格式验证
        validateCode(request.getCode());
        validateName(request.getName());

        if (StringUtils.hasText(request.getDescription())) {
            validateDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getContactPhone())) {
            validatePhone(request.getContactPhone());
        }
        if (StringUtils.hasText(request.getContactEmail())) {
            validateEmail(request.getContactEmail());
        }
        if (request.getCommissionRate() != null) {
            validateCommissionRate(request.getCommissionRate());
        }
        if (request.getMonthlyTarget() != null) {
            validateMonthlyTarget(request.getMonthlyTarget());
        }
        if (request.getSortOrder() != null) {
            validateSortOrder(request.getSortOrder());
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
        if (StringUtils.hasText(request.getContactPhone())) {
            validatePhone(request.getContactPhone());
        }
        if (StringUtils.hasText(request.getContactEmail())) {
            validateEmail(request.getContactEmail());
        }
        if (request.getCommissionRate() != null) {
            validateCommissionRate(request.getCommissionRate());
        }
        if (request.getMonthlyTarget() != null) {
            validateMonthlyTarget(request.getMonthlyTarget());
        }
        if (request.getSortOrder() != null) {
            validateSortOrder(request.getSortOrder());
        }

        log.debug("更新渠道请求验证通过");
    }

    /**
     * 验证删除操作
     *
     * @param channel 待删除的渠道
     * @throws ValidationException 验证失败时抛出
     */
    public void validateDeleteOperation(Channel channel) {
        log.debug("验证删除渠道操作: {}", channel);

        if (channel == null) {
            throw new ValidationException("渠道不存在");
        }

        // 检查渠道状态
        if (channel.isDeleted()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_DELETED,
                "渠道已被删除: " + channel.getCode());
        }

        // 检查是否有关联业务数据
        // TODO: 后续版本中添加订单、销售记录等关联数据检查

        log.debug("删除渠道操作验证通过");
    }

    /**
     * 验证激活操作
     *
     * @param channel 渠道
     * @throws ValidationException 验证失败时抛出
     */
    public void validateActivateOperation(Channel channel) {
        log.debug("验证激活渠道操作: {}", channel);

        if (channel == null) {
            throw new ValidationException("渠道不存在");
        }

        if (channel.isDeleted()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_DELETED,
                "已删除的渠道无法激活: " + channel.getCode());
        }

        if (channel.isActive()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_ACTIVE,
                "渠道已激活: " + channel.getCode());
        }

        // 检查必要的业务信息
        if (!StringUtils.hasText(channel.getContactName()) &&
            !StringUtils.hasText(channel.getContactEmail()) &&
            !StringUtils.hasText(channel.getContactPhone())) {
            log.warn("渠道 {} 缺少联系信息，但仍可激活", channel.getCode());
        }

        log.debug("激活渠道操作验证通过");
    }

    /**
     * 验证停用操作
     *
     * @param channel 渠道
     * @throws ValidationException 验证失败时抛出
     */
    public void validateDeactivateOperation(Channel channel) {
        log.debug("验证停用渠道操作: {}", channel);

        if (channel == null) {
            throw new ValidationException("渠道不存在");
        }

        if (channel.isDeleted()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_DELETED,
                "已删除的渠道无法停用: " + channel.getCode());
        }

        if (!channel.isActive()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_INACTIVE,
                "渠道已停用: " + channel.getCode());
        }

        log.debug("停用渠道操作验证通过");
    }

    /**
     * 验证暂停操作
     *
     * @param channel 渠道
     * @throws ValidationException 验证失败时抛出
     */
    public void validateSuspendOperation(Channel channel) {
        log.debug("验证暂停渠道操作: {}", channel);

        if (channel == null) {
            throw new ValidationException("渠道不存在");
        }

        if (channel.isDeleted()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_DELETED,
                "已删除的渠道无法暂停: " + channel.getCode());
        }

        if (channel.isSuspended()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_SUSPENDED,
                "渠道已暂停: " + channel.getCode());
        }

        log.debug("暂停渠道操作验证通过");
    }

    /**
     * 验证恢复操作
     *
     * @param channel 渠道
     * @throws ValidationException 验证失败时抛出
     */
    public void validateRestoreOperation(Channel channel) {
        log.debug("验证恢复渠道操作: {}", channel);

        if (channel == null) {
            throw new ValidationException("渠道不存在");
        }

        if (channel.isDeleted()) {
            throw new BusinessException(ErrorCode.CHANNEL_ALREADY_DELETED,
                "已删除的渠道无法恢复: " + channel.getCode());
        }

        if (!channel.isSuspended()) {
            throw new BusinessException(ErrorCode.CHANNEL_NOT_SUSPENDED,
                "渠道未暂停，无需恢复: " + channel.getCode());
        }

        log.debug("恢复渠道操作验证通过");
    }

    /**
     * 验证代码唯一性
     *
     * @param code 渠道代码
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
            exists = channelRepository.existsByCodeAndIdNot(code, excludeId);
        } else {
            exists = channelRepository.existsByCode(code);
        }

        if (exists) {
            throw new BusinessException(ErrorCode.CHANNEL_CODE_ALREADY_EXISTS,
                "渠道代码已存在: " + code);
        }

        log.debug("代码唯一性验证通过");
    }

    /**
     * 验证名称唯一性
     *
     * @param name 渠道名称
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
            exists = channelRepository.existsByNameAndIdNot(name, excludeId);
        } else {
            exists = channelRepository.existsByName(name);
        }

        if (exists) {
            throw new BusinessException(ErrorCode.CHANNEL_NAME_ALREADY_EXISTS,
                "渠道名称已存在: " + name);
        }

        log.debug("名称唯一性验证通过");
    }

    /**
     * 验证渠道代码格式
     *
     * @param code 渠道代码
     * @throws ValidationException 验证失败时抛出
     */
    private void validateCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new ValidationException("渠道代码不能为空");
        }

        if (code.length() < 2 || code.length() > 50) {
            throw new ValidationException("渠道代码长度必须在2-50字符之间");
        }

        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new ValidationException("渠道代码只能包含字母、数字和下划线");
        }

        // 检查保留关键字
        if (isReservedKeyword(code)) {
            throw new ValidationException("渠道代码不能使用保留关键字: " + code);
        }
    }

    /**
     * 验证渠道名称格式
     *
     * @param name 渠道名称
     * @throws ValidationException 验证失败时抛出
     */
    private void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new ValidationException("渠道名称不能为空");
        }

        if (name.length() > 100) {
            throw new ValidationException("渠道名称长度不能超过100字符");
        }

        // 检查名称中是否包含危险字符
        if (name.matches(".*[<>\"&].*")) {
            throw new ValidationException("渠道名称不能包含特殊字符: < > \" &");
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
     * 验证电话号码格式
     *
     * @param phone 电话号码
     * @throws ValidationException 验证失败时抛出
     */
    private void validatePhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return;
        }

        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new ValidationException("联系电话格式不正确");
        }
    }

    /**
     * 验证邮箱格式
     *
     * @param email 邮箱地址
     * @throws ValidationException 验证失败时抛出
     */
    private void validateEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException("联系邮箱格式不正确");
        }
    }

    /**
     * 验证佣金率
     *
     * @param commissionRate 佣金率
     * @throws ValidationException 验证失败时抛出
     */
    private void validateCommissionRate(BigDecimal commissionRate) {
        if (commissionRate == null) {
            return;
        }

        if (commissionRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("佣金率不能为负数");
        }

        if (commissionRate.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidationException("佣金率不能超过100%");
        }
    }

    /**
     * 验证月度目标
     *
     * @param monthlyTarget 月度目标
     * @throws ValidationException 验证失败时抛出
     */
    private void validateMonthlyTarget(BigDecimal monthlyTarget) {
        if (monthlyTarget == null) {
            return;
        }

        if (monthlyTarget.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("月度目标不能为负数");
        }

        // 检查是否过大（防止溢出）
        if (monthlyTarget.compareTo(new BigDecimal("9999999999999.99")) > 0) {
            throw new ValidationException("月度目标金额过大");
        }
    }

    /**
     * 验证排序顺序
     *
     * @param sortOrder 排序顺序
     * @throws ValidationException 验证失败时抛出
     */
    private void validateSortOrder(Integer sortOrder) {
        if (sortOrder == null) {
            return;
        }

        if (sortOrder < 0) {
            throw new ValidationException("排序顺序不能为负数");
        }

        if (sortOrder > 99999) {
            throw new ValidationException("排序顺序不能超过99999");
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
            "api", "www", "mail", "ftp", "localhost", "test",
            "default", "null", "undefined", "channel", "channels"
        };

        String lowerCode = code.toLowerCase();
        for (String keyword : reservedKeywords) {
            if (keyword.equals(lowerCode)) {
                return true;
            }
        }
        return false;
    }
}