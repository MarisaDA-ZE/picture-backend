package cloud.marisa.picturebackend.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 运算枚举
 * @date 2025/4/5
 */
@Getter
@ToString
public enum MathComparator implements MrsBaseEnum<String> {
    /**
     * 大于
     */
    GT("gt"),

    /**
     * 小于
     */
    LT("lt"),

    /**
     * 等于
     */
    EQ("eq");

    private final String value;

    MathComparator(String value) {
        this.value = value;
    }
}
