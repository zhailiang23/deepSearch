package com.ynet.mgmt.role.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 角色搜索空间配置请求
 *
 * @author Claude
 */
public class RoleSearchSpaceConfigRequest {

    /**
     * 搜索空间ID列表
     */
    @NotNull(message = "搜索空间ID列表不能为空")
    private List<Long> searchSpaceIds;

    // ========== 构造方法 ==========

    public RoleSearchSpaceConfigRequest() {}

    public RoleSearchSpaceConfigRequest(List<Long> searchSpaceIds) {
        this.searchSpaceIds = searchSpaceIds;
    }

    // ========== Getter & Setter ==========

    public List<Long> getSearchSpaceIds() {
        return searchSpaceIds;
    }

    public void setSearchSpaceIds(List<Long> searchSpaceIds) {
        this.searchSpaceIds = searchSpaceIds;
    }

    @Override
    public String toString() {
        return "RoleSearchSpaceConfigRequest{" +
                "searchSpaceIds=" + searchSpaceIds +
                '}';
    }
}
