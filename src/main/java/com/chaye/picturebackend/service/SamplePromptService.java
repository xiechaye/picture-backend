package com.chaye.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.chaye.picturebackend.model.dto.prompt.SamplePromptQueryRequest;
import com.chaye.picturebackend.model.entity.SamplePrompt;
import com.chaye.picturebackend.model.vo.SamplePromptVO;

import java.util.List;

/**
 * 针对表【sample_prompt(示例提示词)】的数据库操作Service
 */
public interface SamplePromptService extends IService<SamplePrompt> {

    /**
     * 获取多样化随机提示词
     *
     * @param count 数量（最大50）
     * @return 提示词列表（仅 title 和 prompt）
     */
    List<SamplePromptVO> getRandomPrompts(int count);

    /**
     * 校验提示词数据
     *
     * @param samplePrompt 提示词对象
     * @param add          是否为新增操作
     */
    void validSamplePrompt(SamplePrompt samplePrompt, boolean add);

    /**
     * 获取查询条件包装器
     *
     * @param queryRequest 查询请求
     * @return QueryWrapper
     */
    QueryWrapper<SamplePrompt> getQueryWrapper(SamplePromptQueryRequest queryRequest);

    /**
     * 刷新缓存
     */
    void refreshCache();

    /**
     * 获取所有分类（去重）
     *
     * @return 分类列表（从现有提示词中提取去重）
     */
    List<String> getAllCategories();
}
