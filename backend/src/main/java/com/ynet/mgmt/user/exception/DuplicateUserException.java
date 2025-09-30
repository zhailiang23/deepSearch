package com.ynet.mgmt.user.exception;

/**
 * 用户重复异常
 *
 * @author system
 * @since 1.0.0
 */
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }

    public static DuplicateUserException byUsername(String username) {
        return new DuplicateUserException("用户名已存在: " + username);
    }

    public static DuplicateUserException byEmail(String email) {
        return new DuplicateUserException("邮箱已存在: " + email);
    }
}