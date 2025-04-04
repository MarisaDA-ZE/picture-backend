package cloud.marisa.picturebackend.enums;

import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 空间等级的枚举
 * @date 2025/4/4
 */
@Getter
public enum SpaceLevel implements MrsBaseEnum<Integer> {

    /**
     * 普通版
     */
    COMMON(0, 200, 200 * 1024 * 1024L),

    /**
     * 专业版
     */
    PROFESSIONAL(1, 500, 1000 * 1024 * 1024L),

    /**
     * 旗舰版
     */
    FLAGSHIP(2, 1000, 2000 * 1024 * 1024L);

    SpaceLevel(Integer value, Integer maxCount, Long maxSize) {
        this.value = value;
        this.maxCount = maxCount;
        this.maxSize = maxSize;
    }

    /**
     * 枚举类型
     */
    private final Integer value;

    /**
     * 最大存储数量
     */
    private final Integer maxCount;

    /**
     * 最大存储大小
     */
    private final Long maxSize;
}
