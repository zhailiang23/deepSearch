package com.ynet.mgmt.role.exception;

/**
 * 重复角色异常
 *
 * @author Claude
 */
public class DuplicateRoleException extends RuntimeException {

    public DuplicateRoleException(String message) {
        super(message);
    }

    public DuplicateRoleException(String field, String value) {
        super("角色" + field + "已存在: " + value);
    }
}