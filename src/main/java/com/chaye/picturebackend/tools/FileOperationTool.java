package com.chaye.picturebackend.tools;

import cn.hutool.core.io.FileUtil;
import com.chaye.picturebackend.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 文件操作工具类
 *
 * @author chaye
 */
@Component
public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /**
     * 读取文件工具
     * @param fileName
     * @return
     */
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of file to read") String fileName) {
        // 文件路径
        String filePath = FILE_DIR + File.separator + fileName;
        try {
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * 写入文件工具
     * @param fileName
     * @param content
     * @return
     */
    @Tool(description = "Write content to a file")
    public String writeFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
        String filePath = FILE_DIR + File.separator + fileName;
        try {
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}

