package cloud.marisa.picturebackend.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description SortEnum.枚举
 * @date 2025/3/29
 */
@Getter
public enum SortEnum implements MrsBaseEnum<String> {

    /**
     * 升序
     */
    ASC("asc"),

    /**
     * 降序
     */
    DESC("desc");

    SortEnum(String value) {
        this.value = value;
    }

    private final String value;
}
