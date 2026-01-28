package com.chaye.picturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.chaye.picturebackend.config.FileUploadConfig;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * 文件图片上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Resource
    private FileUploadConfig fileUploadConfig;

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        long maxSize = fileUploadConfig.getMaxPictureSize();
        String maxSizeMB = String.valueOf(maxSize / (1024 * 1024));
        ThrowUtils.throwIf(fileSize > maxSize, ErrorCode.PARAMS_ERROR,
                "文件大小不能超过 " + maxSizeMB + "MB");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!fileUploadConfig.getAllowedPictureFormats().contains(fileSuffix),
                ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
