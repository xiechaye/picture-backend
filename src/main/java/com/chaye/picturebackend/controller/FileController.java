package com.chaye.picturebackend.controller;

import com.chaye.picturebackend.annotation.AuthCheck;
import com.chaye.picturebackend.common.BaseResponse;
import com.chaye.picturebackend.common.ResultUtils;
import com.chaye.picturebackend.constant.UserConstant;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.service.FileService;
import com.chaye.picturebackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件接口
 *
 * @author chaye
 */
@RestController
@RequestMapping("/file")
@Slf4j
@Tag(name = "文件模块")
public class FileController {

    @Resource
    private UserService userService;

    @Resource
    private FileService fileService;

    /**
     * 上传头像
     */
    @PostMapping("/upload/avatar")
    @Operation(summary = "上传头像")
    public BaseResponse<String> uploadAvatar(
            @Parameter(description = "头像文件", required = true)
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        // 检查用户是否登录
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        
        String avatarUrl = fileService.uploadAvatar(file);
        return ResultUtils.success(avatarUrl);
    }

    /**
     * 上传通用文件
     *
     * @param file 文件
     * @param uploadPathPrefix 上传路径前缀（可选，默认为common）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传通用文件")
    public BaseResponse<String> uploadFile(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传路径前缀", example = "common")
            @RequestParam(value = "uploadPathPrefix", required = false, defaultValue = "common") String uploadPathPrefix,
            HttpServletRequest request) {
        // 检查用户是否登录
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        
        String fileUrl = fileService.uploadFile(file, uploadPathPrefix);
        return ResultUtils.success(fileUrl);
    }

    /**
     * 测试文件上传到COS存储（管理员）
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "测试文件上传到COS存储（管理员）")
    public BaseResponse<String> testUploadFile(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "上传路径前缀", example = "test")
            @RequestParam(value = "uploadPathPrefix", required = false, defaultValue = "test") String uploadPathPrefix) {
        String fileUrl = fileService.uploadFile(file, uploadPathPrefix);
        return ResultUtils.success(fileUrl);
    }

    /**
     * 测试从COS下载文件（管理员）
     */
    @GetMapping("/test/download/")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @Operation(summary = "测试从COS下载文件（管理员）")
    public void testDownloadFile(
            @Parameter(description = "文件路径", required = true)
            @RequestParam("filepath") String filepath,
            HttpServletResponse response) {
        
        if (filepath == null || filepath.isEmpty()) {
            throw new RuntimeException("文件路径不能为空");
        }
        
        try {
            // 这里可以实现文件下载逻辑
            // 由于需要重写文件下载逻辑，暂时抛出异常提示用户修改
            throw new RuntimeException("文件下载功能需要实现");
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new RuntimeException("文件下载失败");
        }
    }
}