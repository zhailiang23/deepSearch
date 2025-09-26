package com.ynet.mgmt.role.exception;

/**
 * 角色未找到异常
 *
 * @author Claude
 */
public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(String message) {
        super(message);
    }

    public RoleNotFoundException(Long id) {
        super("角色不存在，ID: " + id);
    }

    public RoleNotFoundException(String field, String value) {
        super("角色不存在，" + field + ": " + value);
    }
}