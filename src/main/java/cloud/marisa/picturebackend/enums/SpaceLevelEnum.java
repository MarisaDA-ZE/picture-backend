package cloud.marisa.picturebackend.enums;

import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 空间等级的枚举
 * @date 2025/4/4
 */
@Getter
public enum SpaceLevelEnum implements MrsBaseEnum<Integer> {

    /**
     * 普通版
     */
    COMMON(0, "普通版", 200, 200 * 1024 * 1024L),

    /**
     * 专业版
     */
    PROFESSIONAL(1, "专业版", 500, 500 * 1024 * 1024L),

    /**
     * 旗舰版
     */
    FLAGSHIP(2, "旗舰版", 1000, 1000 * 1024 * 1024L);

    SpaceLevelEnum(Integer value, String text, Integer maxCount, Long maxSize) {
        this.value = value;
        this.text = text;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 枚举类型
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String text;

    /**
     * 最大存储数量
     */
    private final Integer maxCount;

    /**
     * 最大存储大小
     */
    private final Long maxSize;
}
