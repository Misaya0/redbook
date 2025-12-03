package com.itcast.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 本地文件存储工具类（用于开发测试，替代 OSS）
 */
@Data
@Slf4j
public class LocalFileUtil {

    private String uploadPath;  // 文件上传路径
    private String accessUrl;   // 文件访问URL前缀

    /**
     * 上传图片到本地
     * @param bytes 图片字节数组
     * @return 图片访问URL
     */
    public String uploadImg(byte[] bytes) {
        try {
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + ".png";

            // 确保上传目录存在
            File uploadDir = new File(uploadPath);
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
                log.info("创建上传目录: {}", uploadPath);
            }

            // 保存文件
            File file = new File(uploadPath, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(bytes);
            }

            log.info("图片上传成功: {}", fileName);

            // 返回访问URL
            return accessUrl + "/" + fileName;
        } catch (IOException e) {
            log.error("图片上传失败", e);
            throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除本地图片
     * @param imageUrl 图片URL
     */
    public void deleteImg(String imageUrl) {
        try {
            // 从URL中提取文件名
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            Path filePath = Paths.get(uploadPath, fileName);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("图片删除成功: {}", fileName);
            } else {
                log.warn("图片不存在: {}", fileName);
            }
        } catch (Exception e) {
            log.error("删除图片失败: {}", imageUrl, e);
            throw new RuntimeException("删除图片失败: " + imageUrl, e);
        }
    }
}
