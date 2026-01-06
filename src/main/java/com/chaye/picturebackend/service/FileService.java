package com.chaye.picturebackend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务
 */
public interface FileService {

    /**
     * 上传通用文件
     * 
     * @param file 文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 文件访问地址
     */
    String uploadFile(MultipartFile file, String uploadPathPrefix);

    /**
     * 上传头像文件
     * 
     * @param file 头像文件
     * @return 文件访问地址
     */
    String uploadAvatar(MultipartFile file);
}