package com.itcast.config;

import com.itcast.util.LocalFileUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 本地文件存储配置
 */
@Configuration
public class LocalFileConfig {

    @Bean
    @ConfigurationProperties(prefix = "local-file")
    public LocalFileUtil localFileUtil() {
        return new LocalFileUtil();
    }
}
