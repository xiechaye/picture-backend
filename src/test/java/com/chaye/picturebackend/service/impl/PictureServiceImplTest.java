package com.chaye.picturebackend.service.impl;

import com.chaye.picturebackend.model.dto.picture.PictureSearchBySemanticRequest;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.model.vo.PictureVO;
import com.chaye.picturebackend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class PictureServiceImplTest {

    @Resource
    private PictureServiceImpl pictureService;

    @Resource
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 创建一个测试用户（假设用户ID=1存在）
        testUser = new User();
        testUser.setId(2008433726933536770L);
    }

    @Test
    void searchPictureBySemantic() {
        // 1. 测试基本语义搜索（查询公共图库）
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText("一个男孩");

        List<PictureVO> result = pictureService.searchPictureBySemantic(request, testUser);

        assertNotNull(result);
        log.info("搜索 '一个男孩' 返回 {} 条结果", result.size());
        result.forEach(vo -> log.info("图片: id={}, name={}", vo.getId(), vo.getName()));
    }

    @Test
    void searchPictureBySemanticWithTopK() {
        // 2. 测试自定义 topK 参数
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText("风景");
        request.setTopK(5);

        List<PictureVO> result = pictureService.searchPictureBySemantic(request, testUser);

        assertNotNull(result);
        assertTrue(result.size() <= 5, "返回结果应不超过 topK 设定值");
        log.info("搜索 '风景' (topK=5) 返回 {} 条结果", result.size());
    }

    @Test
    void searchPictureBySemanticWithThreshold() {
        // 3. 测试自定义相似度阈值
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText("下雪天的故宫");
        request.setSimilarityThreshold(0.7);  // 较高的相似度阈值

        List<PictureVO> result = pictureService.searchPictureBySemantic(request, testUser);

        assertNotNull(result);
        log.info("搜索 '下雪天的故宫' (threshold=0.7) 返回 {} 条结果", result.size());
    }

    @Test
    void searchPictureBySemanticWithSpaceId() {
        // 4. 测试带空间 ID 过滤（需要用户有权限访问该空间）
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText("风景");
        request.setSpaceId(1L);  // 假设存在 spaceId=1 的空间且用户有权限

        // 注意：如果用户没有权限访问该空间，会抛出异常
        try {
            List<PictureVO> result = pictureService.searchPictureBySemantic(request, testUser);
            assertNotNull(result);
            log.info("搜索 '风景' (spaceId=1) 返回 {} 条结果", result.size());
        } catch (Exception e) {
            log.info("用户没有权限访问空间1，抛出异常: {}", e.getMessage());
        }
    }

    @Test
    void searchPictureBySemanticWithEmptyText() {
        // 5. 测试空搜索文本应抛出异常
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText("");

        assertThrows(Exception.class, () -> {
            pictureService.searchPictureBySemantic(request, testUser);
        }, "空搜索文本应抛出异常");
    }

    @Test
    void searchPictureBySemanticWithNullText() {
        // 6. 测试 null 搜索文本应抛出异常
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText(null);

        assertThrows(Exception.class, () -> {
            pictureService.searchPictureBySemantic(request, testUser);
        }, "null 搜索文本应抛出异常");
    }

    @Test
    void searchPictureBySemanticWithNullUser() {
        // 7. 测试 null 用户应抛出异常
        PictureSearchBySemanticRequest request = new PictureSearchBySemanticRequest();
        request.setSearchText("风景");

        assertThrows(Exception.class, () -> {
            pictureService.searchPictureBySemantic(request, null);
        }, "null 用户应抛出异常");
    }
}
