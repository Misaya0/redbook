package com.itcast.util;

import com.itcast.config.MinioProperties;
import com.itcast.exception.FileIsNullException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;

/**
 * MinIO 文件上传工具类
 */
public class MinioUtil {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioUtil(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * 上传文件到 MinIO
     *
     * @param file MultipartFile 文件对象
     * @return 上传成功后的完整访问 URL
     */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileIsNullException("上传文件不能为空");
        }

        String bucketName = minioProperties.getBucketName();
        String objectName = buildObjectName(file.getOriginalFilename());

        try {
            // 1. 确保 Bucket 存在（不存在则创建）
            ensureBucketExists(bucketName);

            // 2. 上传文件流到 MinIO
            try (InputStream inputStream = file.getInputStream()) {
                PutObjectArgs putObjectArgs = PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build();
                minioClient.putObject(putObjectArgs);
            }

            // 3. 拼接并返回完整可访问 URL
            return buildFileUrl(bucketName, objectName);
        } catch (Exception e) {
            throw new RuntimeException("MinIO 上传失败", e);
        }
    }

    /**
     * 根据完整 URL 删除 MinIO 中的对象
     *
     * @param fileUrl upload 方法返回的完整 URL（形如：endpoint/bucket/object）
     */
    public void delete(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            URI uri = URI.create(fileUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return;
            }

            // path 形如：/bucketName/objectName...
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            int firstSlash = path.indexOf("/");
            if (firstSlash <= 0 || firstSlash >= path.length() - 1) {
                return;
            }

            String bucketName = path.substring(0, firstSlash);
            String objectName = path.substring(firstSlash + 1);

            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("MinIO 删除失败", e);
        }
    }

    private void ensureBucketExists(String bucketName) throws Exception {
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("minio.bucketName 不能为空");
        }

        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private String buildObjectName(String originalFilename) {
        // 使用 UUID 生成唯一文件名，防止重名
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String ext = "";

        if (originalFilename != null) {
            int lastDotIndex = originalFilename.lastIndexOf(".");
            if (lastDotIndex > -1 && lastDotIndex < originalFilename.length() - 1) {
                ext = originalFilename.substring(lastDotIndex);
            }
        }

        return uuid + ext;
    }

    private String buildFileUrl(String bucketName, String objectName) {
        String endpoint = Objects.requireNonNullElse(minioProperties.getEndpoint(), "");
        // 统一去掉末尾的 /
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + bucketName + "/" + objectName;
    }
}
