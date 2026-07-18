package com.sky.utils;

import com.sky.properties.MinIOProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Slf4j
public class MinIOUtil {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinIOProperties minIOProperties;

    /**
     * 文件上传
     *
     * @param inputStream 文件输入流
     * @param objectName  对象名称（文件路径）
     * @return 文件访问 URL
     */
    public String upload(InputStream inputStream, String objectName) {
        try {
            // 检查 bucket 是否存在，不存在则创建
            boolean found = minioClient.bucketExists(
                    io.minio.BucketExistsArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .build()
            );

            if (!found) {
                // 创建 bucket
                minioClient.makeBucket(
                        io.minio.MakeBucketArgs.builder()
                                .bucket(minIOProperties.getBucketName())
                                .build()
                );

                // 设置 bucket 为公开读取策略
                String policy = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": \"*\",\n" +
                        "      \"Action\": \"s3:GetObject\",\n" +
                        "      \"Resource\": \"arn:aws:s3:::" + minIOProperties.getBucketName() + "/*\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";
                minioClient.setBucketPolicy(
                        io.minio.SetBucketPolicyArgs.builder()
                                .bucket(minIOProperties.getBucketName())
                                .config(policy)
                                .build()
                );
                log.info("Bucket [{}] 已设置为公开读取", minIOProperties.getBucketName());
            }

            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minIOProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, inputStream.available(), -1)
                            .contentType("image/jpeg")
                            .build()
            );

            // 返回永久访问路径
            String url = minIOProperties.getEndpoint() + "/" + minIOProperties.getBucketName() + "/" + objectName;
            log.info("文件上传成功，访问路径：{}", url);
            return url;
        } catch (Exception e) {
            log.error("MinIO 文件上传失败：{}", e.getMessage());
            throw new RuntimeException("文件上传失败", e);
        }
    }
}
