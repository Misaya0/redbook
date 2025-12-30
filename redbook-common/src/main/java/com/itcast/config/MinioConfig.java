package com.itcast.config;

import com.itcast.util.MinioUtil;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 自动配置
 * 引入 redbook-common 后即可通过 @Autowired 使用 MinioUtil
 */
@Configuration
@ConditionalOnClass(MinioClient.class)
@ConditionalOnProperty(prefix = "minio", name = "endpoint")
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean
    @ConditionalOnMissingBean
    public MinioClient minioClient(MinioProperties minioProperties) {
        // 创建 MinioClient 客户端
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public MinioUtil minioUtil(MinioClient minioClient, MinioProperties minioProperties) {
        return new MinioUtil(minioClient, minioProperties);
    }
}

