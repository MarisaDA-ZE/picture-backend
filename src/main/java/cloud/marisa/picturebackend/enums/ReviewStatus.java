package cloud.marisa.picturebackend.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;
import lombok.ToString;

/**
 * @author MarisaDAZE
 * @description 审核状态的枚举
 * @date 2025/4/1
 */
@Getter
@ToString
public enum ReviewStatus implements MrsBaseEnum<Integer> {
    PENDING(0, "审核中"),
    PASS(1, "审核通过"),
    REJECT(2, "审核不通过");

    /**
     * 审核状态
     */
    private final Integer value;

    /**
     * 审核描述
     */
    private final String desc;

    ReviewStatus(Integer value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
