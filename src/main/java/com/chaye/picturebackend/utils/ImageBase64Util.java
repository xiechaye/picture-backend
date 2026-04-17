package com.chaye.picturebackend.utils;

import cn.hutool.core.io.IoUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 图片 Base64 编码工具类
 * <p>
 * 用于将图片转换为 Base64 格式，解决阿里云 API 无法访问外部 URL 的问题
 */
@Slf4j
public class ImageBase64Util {

    /**
     * 将本地文件编码为 Base64 格式
     * <p>
     * 格式：data:{mime_type};base64,{base64_data}
     *
     * @param filePath 文件路径
     * @return Base64 编码字符串
     */
    public static String encodeFile(String filePath) throws Exception {
        Path path = Path.of(filePath);
        byte[] fileContent = Files.readAllBytes(path);
        String base64String = Base64.getEncoder().encodeToString(fileContent);
        String mimeType = Files.probeContentType(path);
        if (mimeType == null) {
            mimeType = "image/jpeg";
        }
        return "data:" + mimeType + ";base64," + base64String;
    }

    /**
     * 从 URL 下载图片并编码为 Base64
     * <p>
     * 格式：data:{mime_type};base64,{base64_data}
     *
     * @param imageUrl 图片 URL
     * @return Base64 编码字符串
     */
    public static String encodeUrl(String imageUrl) throws Exception {
        log.info("开始下载图片并转换为 Base64: {}", imageUrl);

        HttpURLConnection connection = null;
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;

        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("下载图片失败，HTTP 状态码: " + responseCode);
            }

            // 获取 Content-Type
            String contentType = connection.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = "image/jpeg";
            }
            // 处理可能的分号参数
            int semicolonIndex = contentType.indexOf(';');
            if (semicolonIndex > 0) {
                contentType = contentType.substring(0, semicolonIndex);
            }

            // 读取图片数据
            inputStream = connection.getInputStream();
            outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            byte[] imageBytes = outputStream.toByteArray();
            String base64String = Base64.getEncoder().encodeToString(imageBytes);

            String result = "data:" + contentType + ";base64," + base64String;
            log.info("Base64 编码完成，原始大小: {} bytes, Base64 大小: {} chars",
                    imageBytes.length, result.length());

            return result;

        } finally {
            IoUtil.close(outputStream);
            IoUtil.close(inputStream);
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 判断字符串是否为 Base64 格式的图片
     */
    public static boolean isBase64Image(String str) {
        return str != null && str.startsWith("data:image");
    }

    /**
     * 从 Base64 字符串中提取 MIME 类型
     */
    public static String getMimeTypeFromBase64(String base64Data) {
        if (base64Data == null || !base64Data.startsWith("data:")) {
            return "image/jpeg";
        }
        int endMimeIndex = base64Data.indexOf(';');
        if (endMimeIndex > 5) {
            return base64Data.substring(5, endMimeIndex);
        }
        return "image/jpeg";
    }
}
