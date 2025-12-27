package com.itcast.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.time.ZoneId;

/**
 * 日期天数差工具类
 */
public class DiffDayUtil {

    /**
     * 计算两个 LocalDateTime 之间的天数差
     */
    public static int diffDays(LocalDateTime date1, LocalDateTime date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(date1.toLocalDate(), date2.toLocalDate());
    }

    /**
     * 计算两个 Date 之间的天数差 (兼容旧代码)
     */
    public static int diffDays(Date date1, Date date2) {
        if (date1 == null || date2 == null) {
            return 0;
        }
        LocalDateTime ldt1 = date1.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime ldt2 = date2.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return diffDays(ldt1, ldt2);
    }
}
