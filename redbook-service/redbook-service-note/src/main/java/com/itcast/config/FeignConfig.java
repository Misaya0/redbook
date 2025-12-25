package com.itcast.config;

import com.itcast.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 配置类
 * 用于在 Feign 调用时传递请求头信息
 */
@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            // 从 UserContext 中获取当前用户 ID
            Long userId = UserContext.getUserId();
            if (userId != null) {
                // 将 userId 添加到请求头中
                template.header("userId", String.valueOf(userId));
                log.info("Feign 调用传递 userId: {}", userId);
            }
        };
    }
}
