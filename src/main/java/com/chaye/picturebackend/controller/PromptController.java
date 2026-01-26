package com.chaye.picturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chaye.picturebackend.annotation.AuthCheck;
import com.chaye.picturebackend.common.BaseResponse;
import com.chaye.picturebackend.common.DeleteRequest;
import com.chaye.picturebackend.common.ResultUtils;
import com.chaye.picturebackend.constant.UserConstant;
import com.chaye.picturebackend.exception.BusinessException;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import com.chaye.picturebackend.model.dto.prompt.SamplePromptAddRequest;
import com.chaye.picturebackend.model.dto.prompt.SamplePromptQueryRequest;
import com.chaye.picturebackend.model.dto.prompt.SamplePromptUpdateRequest;
import com.chaye.picturebackend.model.entity.SamplePrompt;
import com.chaye.picturebackend.model.vo.SamplePromptVO;
import com.chaye.picturebackend.service.SamplePromptService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 示例提示词接口
 */
@Slf4j
@RestController
@RequestMapping("/prompt")
public class PromptController {

    @Resource
    private SamplePromptService samplePromptService;

    // region 用户接口

    /**
     * 获取所有分类列表
     *
     * @return 分类列表（去重后的所有分类）
     */
    @GetMapping("/category/list")
    public BaseResponse<List<String>> getCategories() {
        List<String> categories = samplePromptService.getAllCategories();
        return ResultUtils.success(categories);
    }

    /**
     * 获取多样化随机提示词
     *
     * @param count 数量（默认4，最大50）
     * @return 提示词列表
     */
    @GetMapping("/random")
    public BaseResponse<List<SamplePromptVO>> getRandomPrompts(
            @RequestParam(defaultValue = "4") int count) {
        List<SamplePromptVO> prompts = samplePromptService.getRandomPrompts(count);
        return ResultUtils.success(prompts);
    }

    // endregion

    // region 管理员接口

    /**
     * 新增示例提示词（仅管理员）
     */
    @PostMapping("/admin/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addSamplePrompt(@RequestBody SamplePromptAddRequest addRequest) {
        ThrowUtils.throwIf(addRequest == null, ErrorCode.PARAMS_ERROR);

        SamplePrompt samplePrompt = new SamplePrompt();
        BeanUtils.copyProperties(addRequest, samplePrompt);

        // 校验
        samplePromptService.validSamplePrompt(samplePrompt, true);

        // 保存
        boolean result = samplePromptService.save(samplePrompt);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 刷新缓存
        samplePromptService.refreshCache();

        return ResultUtils.success(samplePrompt.getId());
    }

    /**
     * 更新示例提示词（仅管理员）
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSamplePrompt(@RequestBody SamplePromptUpdateRequest updateRequest) {
        if (updateRequest == null || updateRequest.getId() == null || updateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        SamplePrompt samplePrompt = new SamplePrompt();
        BeanUtils.copyProperties(updateRequest, samplePrompt);

        // 校验
        samplePromptService.validSamplePrompt(samplePrompt, false);

        // 判断是否存在
        SamplePrompt oldPrompt = samplePromptService.getById(updateRequest.getId());
        ThrowUtils.throwIf(oldPrompt == null, ErrorCode.NOT_FOUND_ERROR);

        // 更新
        boolean result = samplePromptService.updateById(samplePrompt);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 刷新缓存
        samplePromptService.refreshCache();

        return ResultUtils.success(true);
    }

    /**
     * 删除示例提示词（仅管理员）
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteSamplePrompt(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 判断是否存在
        SamplePrompt oldPrompt = samplePromptService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(oldPrompt == null, ErrorCode.NOT_FOUND_ERROR);

        // 删除（逻辑删除）
        boolean result = samplePromptService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 刷新缓存
        samplePromptService.refreshCache();

        return ResultUtils.success(true);
    }

    /**
     * 分页查询示例提示词列表（仅管理员）
     */
    @PostMapping("/admin/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<SamplePrompt>> listSamplePromptByPage(
            @RequestBody SamplePromptQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        long current = queryRequest.getCurrent();
        long pageSize = queryRequest.getPageSize();

        // 限制单页大小
        ThrowUtils.throwIf(pageSize > 50, ErrorCode.PARAMS_ERROR, "单页数量不能超过50");

        Page<SamplePrompt> page = samplePromptService.page(
                new Page<>(current, pageSize),
                samplePromptService.getQueryWrapper(queryRequest)
        );

        return ResultUtils.success(page);
    }

    /**
     * 根据ID获取示例提示词详情（仅管理员）
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<SamplePrompt> getSamplePromptById(@RequestParam Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);

        SamplePrompt samplePrompt = samplePromptService.getById(id);
        ThrowUtils.throwIf(samplePrompt == null, ErrorCode.NOT_FOUND_ERROR);

        return ResultUtils.success(samplePrompt);
    }

    // endregion
}
