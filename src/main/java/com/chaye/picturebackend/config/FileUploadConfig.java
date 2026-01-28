package com.chaye.picturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 文件上传配置类
 * 统一管理文件上传相关的大小和格式限制
 */
@Data
@Component
@ConfigurationProperties(prefix = "file.upload")
public class FileUploadConfig {

    /**
     * 图片上传最大大小（字节）
     * 默认：50MB
     */
    private Long maxPictureSize = 50L * 1024 * 1024;

    /**
     * 通用文件上传最大大小（字节）
     * 默认：50MB
     */
    private Long maxFileSize = 50L * 1024 * 1024;

    /**
     * 头像上传最大大小（字节）
     * 默认：5MB
     */
    private Long maxAvatarSize = 5L * 1024 * 1024;

    /**
     * 允许的图片格式列表
     */
    private List<String> allowedPictureFormats = Arrays.asList("jpeg", "png", "jpg", "webp");

    /**
     * 允许的文件格式列表
     */
    private List<String> allowedFileFormats = Arrays.asList("jpeg", "jpg", "png", "gif", "bmp", "webp", "ico");
}
