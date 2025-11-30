package com.itcast.handle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itcast.model.pojo.ExceptionMessage;
import com.itcast.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 全局异常处理
 */
@ControllerAdvice
@ResponseBody
@Slf4j
public class GlobalExceptionHandle {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @ExceptionHandler(value = Exception.class)
    public Result<String> exception(Exception e) {
        try {
            StackTraceElement[] stackTrace = e.getStackTrace();
            StackTraceElement firstStack = stackTrace[0];
            ExceptionMessage exceptionMessage
                    = ExceptionMessage.builder()
                    .className(firstStack.getClassName())
                    .methodName(firstStack.getMethodName())
                    .line(firstStack.getLineNumber())
                    .fileName(firstStack.getFileName())
                    .build();
            log.error("异常发生在: {}.{}({}:{})",
                    firstStack.getClassName(), firstStack.getMethodName(), firstStack.getFileName(), firstStack.getLineNumber());
            log.error("异常详细信息: {}", e.getMessage(), e);
            kafkaTemplate.send("system-exception-log", objectMapper.writeValueAsString(exceptionMessage));
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("记录日志失败:{}", jsonProcessingException.getMessage());
        }
        return Result.failure(e.getMessage());
    }
}
