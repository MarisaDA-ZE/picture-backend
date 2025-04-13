package cloud.marisa.picturebackend.enums;

import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 时间维度的枚举
 * @date 2025/4/12
 */
@Getter
public enum MrsTimeDimension implements MrsBaseEnum<String> {
    DAY("day", 24 * 60 * 60L),
    WEEK("week", 7 * 24 * 60 * 60L),
    MONTH("month", 30 * 24 * 60 * 60L);

    /**
     * 名字
     */
    private final String value;

    /**
     * 秒数
     */
    private final Long timeSecond;

    MrsTimeDimension(String value, Long timeSecond) {
        this.value = value;
        this.timeSecond = timeSecond;
    }
}
