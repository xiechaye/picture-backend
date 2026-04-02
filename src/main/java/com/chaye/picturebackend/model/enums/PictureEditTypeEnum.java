package com.chaye.picturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 图片编辑类型枚举类
 */
@Getter
public enum PictureEditTypeEnum {

    /**
     * 智能抠图
     */
    SEGMENT("智能抠图", "SEGMENT"),
    /**
     * 水印去除
     */
    REMOVE_WATERMARK("水印去除", "REMOVE_WATERMARK"),
    /**
     * 图片增强
     */
    ENHANCE("图片增强", "ENHANCE");

    private final String text;

    private final String value;

    PictureEditTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的 value
     * @return 枚举值
     */
    public static PictureEditTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (PictureEditTypeEnum editTypeEnum : PictureEditTypeEnum.values()) {
            if (editTypeEnum.value.equals(value)) {
                return editTypeEnum;
            }
        }
        return null;
    }
}
