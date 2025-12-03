package com.itcast.config;

import com.itcast.interceptor.MyInterceptor;
import lombok.extern.slf4j.Slf4j;
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
                .excludePathPatterns(
                        "/user/send/**",
                        "/user/verify",
                        "/user/getAttention/**",
                        "/user/getUserById/**",
                        "/user/uploads/**", // 放行图片访问
                        // Swagger / OpenAPI 相关路径放行
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                );
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        // 映射本地文件上传路径
        String uploadPath = "file:" + System.getProperty("user.dir") + "/uploads/";
        log.info("Configuring static resource handler for path: /user/uploads/** -> {}", uploadPath);
        registry.addResourceHandler("/user/uploads/**")
                .addResourceLocations(uploadPath);
    }
}
