package com.itcast.config;

import com.itcast.interceptor.MyInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

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
                .excludePathPatterns("/user/send/**", "/user/verify", "/note/uploads/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 动态查找 uploads 目录
        String userDir = System.getProperty("user.dir");
        String uploadPath = userDir + File.separator + "uploads";
        File uploadDir = new File(uploadPath);

        // 如果当前目录下没有 uploads，尝试去父目录找（应对模块化启动的情况）
        if (!uploadDir.exists()) {
            File parentDir = new File(userDir).getParentFile();
            if (parentDir != null) {
                File parentUploadDir = new File(parentDir, "uploads");
                if (parentUploadDir.exists()) {
                    uploadPath = parentUploadDir.getAbsolutePath();
                }
            }
        }

        // 统一处理路径分隔符，确保 file: 协议正确
        uploadPath = uploadPath.replace("\\", "/");
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        log.info("配置静态资源映射，上传路径: {}", uploadPath);
        
        // 将 /note/uploads/** 映射到本地文件系统
        registry.addResourceHandler("/note/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
