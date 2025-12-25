package com.itcast.context;

/**
 * 用户线程变量
 */
public class UserContext {
    public static final ThreadLocal<Long> userThreadLocal = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        userThreadLocal.set(userId);
    }

    public static Long getUserId() {
        return userThreadLocal.get();
    }

    public static void clear() {
        userThreadLocal.remove();
    }
}
