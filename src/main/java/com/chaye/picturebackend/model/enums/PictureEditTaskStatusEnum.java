package com.chaye.picturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片编辑任务状态枚举类
 */
@Getter
public enum PictureEditTaskStatusEnum {

    PROCESSING("处理中", "PROCESSING"),
    SUCCESS("成功", "SUCCESS"),
    FAILED("失败", "FAILED");

    private final String text;

    private final String value;

    PictureEditTaskStatusEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static PictureEditTaskStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditTaskStatusEnum statusEnum : PictureEditTaskStatusEnum.values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
