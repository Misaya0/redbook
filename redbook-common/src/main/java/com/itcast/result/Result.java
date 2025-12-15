package com.itcast.result;

import lombok.Data;

@Data
public class Result<T> {

    /**
     * 结果数据
     */
    private T data;

    /**
     * 操作消息
     */
    private String message;

    /**
     * 状态码
     */
    private int code;

    // 无参构造，给 Jackson 用
    public Result() {
    }

    /**
     * 总条数
     */
    private Long total;

    public Result(String message, int code, T data) {
        this.message = message;
        this.code = code;
        this.data = data;
    }

    public Result(String message, int code, T data, Long total) {
        this.message = message;
        this.code = code;
        this.data = data;
        this.total = total;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>("操作成功", 200, data);
    }

    public static <T> Result<T> success(T data, Long total) {
        return new Result<>("操作成功", 200, data, total);
    }

    public static <T> Result<T> failure(String message) {
        return new Result<>(message, 400, null);
    }
}

