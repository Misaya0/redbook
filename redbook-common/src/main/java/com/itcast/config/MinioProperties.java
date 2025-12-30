package com.itcast.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置属性
 * 通过 application.yml 中的 minio.* 进行配置
 */
@Data
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {

    /**
     * MinIO 服务地址，例如：http://localhost:9000
     */
    private String endpoint;

    /**
     * 访问密钥
     */
    private String accessKey;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 默认 Bucket 名称
     */
    private String bucketName;
}

