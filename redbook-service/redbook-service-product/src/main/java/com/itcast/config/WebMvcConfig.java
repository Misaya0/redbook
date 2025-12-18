package com.itcast.config;

import com.itcast.interceptor.MyInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
@Slf4j
public class WebMvcConfig implements WebMvcConfigurer {

    private final MyInterceptor myInterceptor;

    public WebMvcConfig(MyInterceptor myInterceptor) {
        this.myInterceptor = myInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(myInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/product/uploads/**",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String userDir = System.getProperty("user.dir");
        String uploadPath = userDir + File.separator + "uploads";
        File uploadDir = new File(uploadPath);

        if (!uploadDir.exists()) {
            File parentDir = new File(userDir).getParentFile();
            if (parentDir != null) {
                File parentUploadDir = new File(parentDir, "uploads");
                if (parentUploadDir.exists()) {
                    uploadPath = parentUploadDir.getAbsolutePath();
                }
            }
        }

        uploadPath = uploadPath.replace("\\", "/");
        if (!uploadPath.endsWith("/")) {
            uploadPath += "/";
        }

        log.info("Configuring static resource handler for path: /product/uploads/** -> file:{}", uploadPath);

        registry.addResourceHandler("/product/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}
