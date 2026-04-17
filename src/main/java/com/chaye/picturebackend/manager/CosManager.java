package com.chaye.picturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.chaye.picturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;
import java.net.URL;
import java.util.Date;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 下载对象到临时文件
     *
     * @param key 唯一键
     * @return 临时文件
     */
    public File downloadToTempFile(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        COSObject cosObject = cosClient.getObject(getObjectRequest);
        try {
            // 创建临时文件
            String suffix = getFileExtension(key);
            File tempFile = File.createTempFile("cos_download_", suffix);
            // 复制内容到临时文件
            cosObject.getObjectContent().transferTo(java.nio.file.Files.newOutputStream(tempFile.toPath()));
            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("下载文件到临时目录失败: " + e.getMessage(), e);
        } finally {
            // 关闭流
            try {
                if (cosObject.getObjectContent() != null) {
                    cosObject.getObjectContent().close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param filename 文件名
     * @return 扩展名（包含点号）
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return ".tmp";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filename.length() - 1) {
            return filename.substring(lastDotIndex);
        }
        return ".tmp";
    }

    /**
     * 上传对象（仅获取原图信息，不进行预处理）
     * 缩略图改用 COS 实时处理，通过 URL 参数动态生成
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 仅获取原图信息，不进行任何预处理（缩略图、WebP 转换等）
        // 前端通过 URL 参数实时获取不同尺寸的图片
        // 例如：{url}?imageMogr2/thumbnail/256x256>
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息（宽高、格式等）
        picOperations.setIsPicInfo(1);
        putObjectRequest.setPicOperations(picOperations);
        try {
            return cosClient.putObject(putObjectRequest);
        } catch (Exception e) {
            System.err.println("COS Upload Error - Bucket: " + cosClientConfig.getBucket());
            System.err.println("COS Upload Error - Key: " + key);
            System.err.println("COS Upload Error - Message: " + e.getMessage());
            System.err.println("COS Upload Error - Type: " + e.getClass().getSimpleName());
            if (e.getCause() != null) {
                System.err.println("COS Upload Error - Cause: " + e.getCause().getMessage());
            }
            throw e;
        }
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }

    /**
     * 生成预签名 URL
     * <p>
     * 用于临时授权访问私有桶中的文件，适用于 AI 服务访问图片
     *
     * @param key        唯一键
     * @param expiration 过期时间（秒），默认 3600 秒（1 小时）
     * @return 预签名 URL
     */
    public String generatePresignedUrl(String key, long expiration) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                cosClientConfig.getBucket(),
                key,
                HttpMethodName.GET
        );
        // 设置过期时间
        request.setExpiration(new Date(System.currentTimeMillis() + expiration * 1000L));
        URL url = cosClient.generatePresignedUrl(request);
        return url.toString();
    }

    /**
     * 生成预签名 URL（默认 1 小时有效期）
     *
     * @param key 唯一键
     * @return 预签名 URL
     */
    public String generatePresignedUrl(String key) {
        return generatePresignedUrl(key, 3600);
    }
}
