package com.ynet.mgmt.user.exception;

/**
 * 用户未找到异常
 *
 * @author system
 * @since 1.0.0
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("用户不存在: id=" + id);
    }

    public UserNotFoundException(String username) {
        super("用户不存在: username=" + username);
    }
}