package com.aicv.airesume.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * 阿里云OSS工具类
 */
@Component
public class OssUtils {

    @Value("${oss.endpoint}")
    private String endpoint;

    @Value("${oss.access-key}")
    private String accessKey;

    @Value("${oss.secret-key}")
    private String secretKey;

    @Value("${oss.bucket-name}")
    private String bucketName;

    /**
     * 上传文件到OSS
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            String fileName = folder + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            InputStream inputStream = file.getInputStream();
            ossClient.putObject(bucketName, fileName, inputStream);
            return "https://" + bucketName + "." + endpoint + "/" + fileName;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 上传文件流到OSS
     */
    public String uploadFileStream(InputStream inputStream, String fileName, String folder) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            String objectName = folder + "/" + fileName;
            ossClient.putObject(bucketName, objectName, inputStream);
            return "https://" + bucketName + "." + endpoint + "/" + objectName;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 删除OSS文件
     */
    public void deleteFile(String fileUrl) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            String objectName = fileUrl.substring(fileUrl.lastIndexOf("/"));
            ossClient.deleteObject(bucketName, objectName);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 从OSS下载文件
     */
    public byte[] downloadFile(String fileId) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKey, secretKey);
        try {
            // 从fileId提取对象名称（移除域名部分，只保留路径和文件名）
            String objectName;
            if (fileId.contains("/")) {
                // 如果是完整的URL，提取路径部分
                int lastSlashIndex = fileId.lastIndexOf("/");
                objectName = fileId.substring(fileId.indexOf("/", 8) + 1); // 跳过 https://
            } else {
                // 如果只是文件名，直接使用
                objectName = fileId;
            }
            
            // 下载文件到字节数组（兼容Java 8）
            try (InputStream inputStream = ossClient.getObject(bucketName, objectName).getObjectContent();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                
                return outputStream.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("下载文件失败: " + e.getMessage(), e);
        } finally {
            ossClient.shutdown();
        }
    }
}