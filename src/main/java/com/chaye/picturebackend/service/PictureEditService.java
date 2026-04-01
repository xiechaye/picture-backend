package com.chaye.picturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chaye.picturebackend.model.dto.pictureedit.PictureEnhanceRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureRemoveWatermarkRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureReplaceBackgroundRequest;
import com.chaye.picturebackend.model.dto.pictureedit.PictureSegmentRequest;
import com.chaye.picturebackend.model.entity.PictureEditTask;
import com.chaye.picturebackend.model.entity.User;
import com.chaye.picturebackend.model.vo.PictureEditTaskVO;

/**
 * 图片编辑服务
 */
public interface PictureEditService extends IService<PictureEditTask> {

    /**
     * 智能抠图
     *
     * @param request    抠图请求
     * @param loginUser  登录用户
     * @return 编辑任务
     */
    PictureEditTaskVO segmentImage(PictureSegmentRequest request, User loginUser);

    /**
     * 去除水印
     *
     * @param request    去水印请求
     * @param loginUser  登录用户
     * @return 编辑任务
     */
    PictureEditTaskVO removeWatermark(PictureRemoveWatermarkRequest request, User loginUser);

    /**
     * 图片增强
     *
     * @param request    增强请求
     * @param loginUser  登录用户
     * @return 编辑任务
     */
    PictureEditTaskVO enhanceImage(PictureEnhanceRequest request, User loginUser);

    /**
     * 背景替换
     *
     * @param request    背景替换请求
     * @param loginUser  登录用户
     * @return 编辑任务
     */
    PictureEditTaskVO replaceBackground(PictureReplaceBackgroundRequest request, User loginUser);

    /**
     * 查询编辑任务状态
     *
     * @param taskId 任务ID
     * @return 编辑任务
     */
    PictureEditTaskVO getTaskStatus(String taskId);

    /**
     * 轮询并更新任务状态
     * 用于后台异步任务处理
     *
     * @param taskId 任务ID
     * @return 是否处理完成
     */
    boolean pollAndUpdateTaskStatus(String taskId);
}
