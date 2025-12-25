package com.itcast.config;

import com.itcast.context.UserContext;
import com.itcast.interceptor.MyInterceptor;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 将拦截器添加到容器中
 */
@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册自定义拦截器");
        registry.addInterceptor(new MyInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/user/send/**", "/user/verify");
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            Long userId = UserContext.getUserId();
            if (userId != null) {
                template.header("userId", String.valueOf(userId));
            }
        };
    }
}
