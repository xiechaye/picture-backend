package com.chaye.picturebackend.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.chaye.picturebackend.config.CosClientConfig;
import com.chaye.picturebackend.config.FileUploadConfig;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import com.chaye.picturebackend.manager.CosManager;
import com.chaye.picturebackend.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;

/**
 * 文件服务实现类
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;

    @Resource
    private FileUploadConfig fileUploadConfig;

    @Override
    public String uploadFile(MultipartFile file, String uploadPathPrefix) {
        // 校验文件
        validFile(file, fileUploadConfig.getMaxFileSize());

        // 生成上传路径
        String uploadPath = generateUploadPath(file, uploadPathPrefix);

        // 上传文件
        return uploadToCos(file, uploadPath);
    }

    @Override
    public String uploadAvatar(MultipartFile file) {
        // 校验头像文件
        validAvatar(file);

        // 生成头像上传路径（统一存储在avatar目录下）
        String uploadPath = generateUploadPath(file, "avatar");

        // 上传文件
        return uploadToCos(file, uploadPath);
    }

    /**
     * 校验通用文件
     */
    private void validFile(MultipartFile multipartFile, long maxSize) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize == 0, ErrorCode.PARAMS_ERROR, "文件不能为空");
        String maxSizeMB = String.valueOf(maxSize / (1024 * 1024));
        ThrowUtils.throwIf(fileSize > maxSize, ErrorCode.PARAMS_ERROR,
                "文件大小不能超过 " + maxSizeMB + "MB");

        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(fileSuffix == null || fileSuffix.isEmpty(), ErrorCode.PARAMS_ERROR, "文件后缀名不能为空");

        // 3. 验证文件后缀是否合法
        ThrowUtils.throwIf(!fileUploadConfig.getAllowedFileFormats().contains(fileSuffix.toLowerCase()),
                ErrorCode.PARAMS_ERROR, "不支持的文件类型，仅支持 " + fileUploadConfig.getAllowedFileFormats());
    }

    /**
     * 校验头像文件
     */
    private void validAvatar(MultipartFile multipartFile) {
        validFile(multipartFile, fileUploadConfig.getMaxAvatarSize());

        // 头像额外校验：必须是图片格式
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        if (!fileUploadConfig.getAllowedPictureFormats().contains(fileSuffix.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像必须是图片格式");
        }
    }

    /**
     * 生成上传路径
     */
    private String generateUploadPath(MultipartFile file, String uploadPathPrefix) {
        String uuid = RandomUtil.randomString(16);
        String originalFilename = file.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originalFilename));
        return String.format("/%s/%s", uploadPathPrefix, uploadFilename);
    }

    /**
     * 上传到COS
     */
    private String uploadToCos(MultipartFile file, String uploadPath) {
        File tempFile = null;
        try {
            // 创建临时文件
            tempFile = File.createTempFile("upload_", null);
            file.transferTo(tempFile);

            // 如果是头像文件，使用普通上传（不进行图片处理）
            if (uploadPath.contains("/avatar/")) {
                PutObjectResult putObjectResult = cosManager.putObject(uploadPath, tempFile);
            } else {
                // 如果是图片文件，使用图片上传（进行图片处理）
                String fileSuffix = FileUtil.getSuffix(file.getOriginalFilename());
                if (fileUploadConfig.getAllowedPictureFormats().contains(fileSuffix.toLowerCase())) {
                    cosManager.putPictureObject(uploadPath, tempFile);
                } else {
                    // 非图片文件使用普通上传
                    cosManager.putObject(uploadPath, tempFile);
                }
            }

            // 返回文件访问地址
            return cosClientConfig.getHost() + "/" + uploadPath;
        } catch (Exception e) {
            log.error("文件上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 清理临时文件
            deleteTempFile(tempFile);
        }
    }

    /**
     * 清理临时文件
     */
    private void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("临时文件删除失败, 文件路径 = {}", file.getAbsolutePath());
        }
    }
}
