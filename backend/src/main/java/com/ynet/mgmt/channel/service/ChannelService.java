package com.ynet.mgmt.channel.service;

import com.ynet.mgmt.channel.dto.ChannelDTO;
import com.ynet.mgmt.channel.dto.CreateChannelRequest;
import com.ynet.mgmt.channel.dto.UpdateChannelRequest;
import com.ynet.mgmt.channel.entity.ChannelStatus;
import com.ynet.mgmt.channel.entity.ChannelType;
import com.ynet.mgmt.common.dto.PageResult;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 渠道业务服务接口
 * 提供渠道管理的核心业务功能
 *
 * @author system
 * @since 1.0.0
 */
public interface ChannelService {

    // ========== 基本CRUD操作 ==========

    /**
     * 创建渠道
     * @param request 创建请求
     * @return 创建的渠道
     */
    ChannelDTO createChannel(CreateChannelRequest request);

    /**
     * 删除渠道
     * @param id 渠道ID
     */
    void deleteChannel(Long id);

    /**
     * 更新渠道
     * @param id 渠道ID
     * @param request 更新请求
     * @return 更新后的渠道
     */
    ChannelDTO updateChannel(Long id, UpdateChannelRequest request);

    /**
     * 根据ID获取渠道
     * @param id 渠道ID
     * @return 渠道信息
     */
    ChannelDTO getChannel(Long id);

    /**
     * 根据代码获取渠道
     * @param code 渠道代码
     * @return 渠道信息
     */
    ChannelDTO getChannelByCode(String code);

    /**
     * 分页查询渠道列表
     * @param pageable 分页参数
     * @return 分页结果
     */
    PageResult<ChannelDTO> listChannels(Pageable pageable);

    // ========== 状态管理 ==========

    /**
     * 激活渠道
     * @param id 渠道ID
     * @return 更新后的渠道
     */
    ChannelDTO activateChannel(Long id);

    /**
     * 停用渠道
     * @param id 渠道ID
     * @return 更新后的渠道
     */
    ChannelDTO deactivateChannel(Long id);

    /**
     * 暂停渠道
     * @param id 渠道ID
     * @return 更新后的渠道
     */
    ChannelDTO suspendChannel(Long id);

    /**
     * 恢复渠道
     * @param id 渠道ID
     * @return 更新后的渠道
     */
    ChannelDTO restoreChannel(Long id);

    // ========== 查询方法 ==========

    /**
     * 获取所有激活渠道列表
     * @return 激活渠道列表
     */
    List<ChannelDTO> getActiveChannels();

    /**
     * 根据类型获取渠道列表
     * @param type 渠道类型
     * @return 渠道列表
     */
    List<ChannelDTO> getChannelsByType(ChannelType type);

    /**
     * 根据状态获取渠道列表
     * @param status 渠道状态
     * @return 渠道列表
     */
    List<ChannelDTO> getChannelsByStatus(ChannelStatus status);

    // ========== 验证方法 ==========

    /**
     * 检查代码是否可用
     * @param code 渠道代码
     * @return 是否可用
     */
    boolean isCodeAvailable(String code);

    /**
     * 检查名称是否可用
     * @param name 渠道名称
     * @return 是否可用
     */
    boolean isNameAvailable(String name);

    /**
     * 检查代码是否可用（排除指定ID）
     * @param code 渠道代码
     * @param excludeId 排除的渠道ID
     * @return 是否可用
     */
    boolean isCodeAvailable(String code, Long excludeId);

    /**
     * 检查名称是否可用（排除指定ID）
     * @param name 渠道名称
     * @param excludeId 排除的渠道ID
     * @return 是否可用
     */
    boolean isNameAvailable(String name, Long excludeId);
}