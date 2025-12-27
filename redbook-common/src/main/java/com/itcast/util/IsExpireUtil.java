package com.itcast.util;

import java.time.LocalDateTime;
import java.util.Date;
import java.time.ZoneId;

/**
 * 判断是否过期
 */
public class IsExpireUtil {

    /**
     * 判断 LocalDateTime 是否过期
     */
    public static boolean isExpire(LocalDateTime expireTime) {
        if (expireTime == null) {
            return false;
        }
        return expireTime.isBefore(LocalDateTime.now());
    }

    /**
     * 判断 Date 是否过期 (兼容旧代码)
     */
    public static boolean isExpire(Date expireTime) {
        if (expireTime == null) {
            return false;
        }
        LocalDateTime ldt = expireTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return isExpire(ldt);
    }
}
