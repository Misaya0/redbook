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
                .excludePathPatterns("/product/uploads/**");
    }

    @Override
    public void addResourceHandlers(org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry registry) {
        // 动态查找 uploads 目录
        String userDir = System.getProperty("user.dir");
        String uploadPath = userDir + java.io.File.separator + "uploads";
        java.io.File uploadDir = new java.io.File(uploadPath);

        // 如果当前目录下没有 uploads，尝试去父目录找
        if (!uploadDir.exists()) {
            java.io.File parentDir = new java.io.File(userDir).getParentFile();
            if (parentDir != null) {
                java.io.File parentUploadDir = new java.io.File(parentDir, "uploads");
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
        
        // 映射 /product/uploads/** 到本地文件
        // 注意：addResourceHandler 匹配的是 URL 路径，addResourceLocations 指定的是本地文件路径
        // 如果文件在 e:/java/RedBook-master/uploads/xxx.png
        // 访问 /product/uploads/xxx.png 时，Spring 会去 uploadPath + xxx.png 查找
        registry.addResourceHandler("/product/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
