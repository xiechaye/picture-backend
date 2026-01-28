package com.chaye.picturebackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.chaye.picturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.File;

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
}
