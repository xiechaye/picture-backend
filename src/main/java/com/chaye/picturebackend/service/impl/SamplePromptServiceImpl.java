package com.chaye.picturebackend.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chaye.picturebackend.exception.ErrorCode;
import com.chaye.picturebackend.exception.ThrowUtils;
import com.chaye.picturebackend.mapper.SamplePromptMapper;
import com.chaye.picturebackend.model.dto.prompt.SamplePromptQueryRequest;
import com.chaye.picturebackend.model.entity.SamplePrompt;
import com.chaye.picturebackend.model.vo.SamplePromptVO;
import com.chaye.picturebackend.service.SamplePromptService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 针对表【sample_prompt(示例提示词)】的数据库操作Service实现
 */
@Slf4j
@Service
public class SamplePromptServiceImpl extends ServiceImpl<SamplePromptMapper, SamplePrompt>
        implements SamplePromptService {

    /**
     * 最大返回数量限制
     */
    private static final int MAX_COUNT = 50;

    /**
     * 缓存：按分类分组存储提示词
     */
    private final Map<String, List<SamplePrompt>> promptCache = new ConcurrentHashMap<>();

    /**
     * 初始化缓存
     */
    @PostConstruct
    public void initCache() {
        refreshCache();
    }

    @Override
    public void refreshCache() {
        // 查询所有未删除的提示词
        List<SamplePrompt> allPrompts = this.list();

        // 按分类分组
        Map<String, List<SamplePrompt>> grouped = allPrompts.stream()
                .filter(p -> StrUtil.isNotBlank(p.getCategory()))
                .collect(Collectors.groupingBy(SamplePrompt::getCategory));

        // 清空并更新缓存
        promptCache.clear();
        promptCache.putAll(grouped);

        log.info("提示词缓存已刷新，共 {} 条数据，{} 个分类",
                allPrompts.size(), grouped.size());
    }

    @Override
    public List<SamplePromptVO> getRandomPrompts(int count) {
        // 参数校验
        ThrowUtils.throwIf(count <= 0, ErrorCode.PARAMS_ERROR, "数量必须大于0");
        count = Math.min(count, MAX_COUNT);

        // 缓存为空时返回空列表
        if (promptCache.isEmpty()) {
            return Collections.emptyList();
        }

        // 获取所有分类
        List<String> categories = new ArrayList<>(promptCache.keySet());

        // 为每个分类创建打乱后的副本
        Map<String, List<SamplePrompt>> shuffledCache = new HashMap<>();
        for (Map.Entry<String, List<SamplePrompt>> entry : promptCache.entrySet()) {
            List<SamplePrompt> shuffled = new ArrayList<>(entry.getValue());
            Collections.shuffle(shuffled);
            shuffledCache.put(entry.getKey(), shuffled);
        }

        // Round-Robin 轮询取数据，最大化分类多样性
        List<SamplePrompt> result = new ArrayList<>();
        Map<String, Integer> categoryIndex = new HashMap<>(); // 记录每个分类当前取到的索引

        int categoryCount = categories.size();
        int totalAvailable = shuffledCache.values().stream().mapToInt(List::size).sum();
        int targetCount = Math.min(count, totalAvailable);

        int currentCategoryIdx = 0;
        while (result.size() < targetCount) {
            String category = categories.get(currentCategoryIdx % categoryCount);
            List<SamplePrompt> categoryList = shuffledCache.get(category);
            int idx = categoryIndex.getOrDefault(category, 0);

            if (idx < categoryList.size()) {
                result.add(categoryList.get(idx));
                categoryIndex.put(category, idx + 1);
            }

            currentCategoryIdx++;

            // 防止死循环：如果所有分类都已取完，退出
            if (currentCategoryIdx >= categoryCount * targetCount) {
                break;
            }
        }

        // 转换为 VO
        return result.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public void validSamplePrompt(SamplePrompt samplePrompt, boolean add) {
        ThrowUtils.throwIf(samplePrompt == null, ErrorCode.PARAMS_ERROR);

        String title = samplePrompt.getTitle();
        String prompt = samplePrompt.getPrompt();
        String category = samplePrompt.getCategory();

        // 新增时必填校验
        if (add) {
            ThrowUtils.throwIf(StrUtil.isBlank(title), ErrorCode.PARAMS_ERROR, "标题不能为空");
            ThrowUtils.throwIf(StrUtil.isBlank(prompt), ErrorCode.PARAMS_ERROR, "提示词不能为空");
            ThrowUtils.throwIf(StrUtil.isBlank(category), ErrorCode.PARAMS_ERROR, "分类不能为空");
        }

        // 长度校验
        if (StrUtil.isNotBlank(title)) {
            ThrowUtils.throwIf(title.length() > 128, ErrorCode.PARAMS_ERROR, "标题过长");
        }
        if (StrUtil.isNotBlank(category)) {
            ThrowUtils.throwIf(category.length() > 64, ErrorCode.PARAMS_ERROR, "分类名称过长");
        }
    }

    @Override
    public QueryWrapper<SamplePrompt> getQueryWrapper(SamplePromptQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);

        Long id = queryRequest.getId();
        String title = queryRequest.getTitle();
        String prompt = queryRequest.getPrompt();
        String category = queryRequest.getCategory();
        String sortField = queryRequest.getSortField();
        String sortOrder = queryRequest.getSortOrder();

        QueryWrapper<SamplePrompt> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.like(StrUtil.isNotBlank(title), "title", title);
        queryWrapper.like(StrUtil.isNotBlank(prompt), "prompt", prompt);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);

        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            queryWrapper.orderBy(true, "ascend".equals(sortOrder), sortField);
        } else {
            queryWrapper.orderByDesc("createTime");
        }

        return queryWrapper;
    }

    /**
     * 将实体转换为 VO
     */
    private SamplePromptVO toVO(SamplePrompt samplePrompt) {
        SamplePromptVO vo = new SamplePromptVO();
        vo.setTitle(samplePrompt.getTitle());
        vo.setPrompt(samplePrompt.getPrompt());
        return vo;
    }
}
