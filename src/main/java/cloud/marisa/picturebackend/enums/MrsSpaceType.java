package cloud.marisa.picturebackend.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 空间类型的枚举
 * @date 2025/4/13
 */
@Getter
public enum MrsSpaceType implements MrsBaseEnum<Integer> {


    /**
     * 私有空间
     */
    PRIVATE(0, "private"),

    /**
     * 团队空间
     */
    TEAM(1, "team");

    MrsSpaceType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * 角色value
     */
    private final Integer value;

    /**
     * 角色名
     */
    private final String name;

}
