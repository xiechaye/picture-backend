package com.chaye.picturebackend.controller;

import com.chaye.picturebackend.service.ImageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author chaye
 *
 * 用于图片在向量数据库中相关操作
 */
@Slf4j
@RestController
@RequestMapping("/images")
@AllArgsConstructor
public class ImageController {
    private final ImageService imageService;

}
