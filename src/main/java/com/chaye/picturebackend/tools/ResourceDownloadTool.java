package com.chaye.picturebackend.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.chaye.picturebackend.config.CosClientConfig;
import com.chaye.picturebackend.manager.CosManager;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

/**
 * 资源下载工具类
 *
 * @author chaye
 */
@Component
public class ResourceDownloadTool {

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Tool(description = "Download a resource from a given URL and upload to COS")
    public String downloadResource(@ToolParam(description = "URL of the resource to download") String url,
                                   @ToolParam(description = "Name of the file to save in COS") String fileName) {
        File tempFile = null;
        try {
            // 使用 Hutool 的 HttpUtil 直接获取字节数组
            byte[] fileBytes;
            try {
                fileBytes = HttpUtil.downloadBytes(url);
            } catch (Exception e) {
                return "Error downloading resource: " + e.getMessage();
            }

            if (fileBytes == null || fileBytes.length == 0) {
                return "Error: Downloaded file is empty";
            }

            // 创建临时文件
            tempFile = createTempFile(fileBytes, fileName);

            // 生成 COS 对象键（key）
            String objectKey = System.currentTimeMillis() + "/" + fileName;

            // 上传到腾讯云 COS
            PutObjectResult putObjectResult = cosManager.putObject(objectKey, tempFile);

            if (putObjectResult == null) {
                return "Error uploading to COS: Upload result is null";
            }

            // 构建完整的 COS URL
            String cosUrl = cosClientConfig.getHost() + "/" + objectKey;

            return "Resource downloaded and uploaded to COS successfully: " + cosUrl;
        } catch (Exception e) {
            return "Error processing resource: " + e.getMessage();
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                FileUtil.del(tempFile);
            }
        }
    }

    /**
     * 创建临时文件
     *
     * @param fileBytes 文件字节数组
     * @param fileName  文件名
     * @return 临时文件
     * @throws IOException IO异常
     */
    private File createTempFile(byte[] fileBytes, String fileName) throws IOException {
        // 获取文件扩展名
        String suffix = FileUtil.getSuffix(fileName);
        if (suffix == null || suffix.isEmpty()) {
            suffix = "tmp";
        }

        // 创建临时文件
        File tempFile = File.createTempFile("download_", "." + suffix);

        // 写入字节数组
        FileUtil.writeBytes(fileBytes, tempFile);

        return tempFile;
    }
}

