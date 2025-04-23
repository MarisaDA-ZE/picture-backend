package cloud.marisa.picturebackend.enums.notice;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;


/**
 * @author MarisaDAZE
 * @description 消息通知类型的枚举
 * @date 2025/4/20
 */
@Getter
public enum MrsNoticeType implements MrsBaseEnum<Integer> {

    SYSTEM(0, "系统通知"),
    USER(1, "用户通知");

    MrsNoticeType(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 通知类型
     */
    private final Integer value;

    /**
     * 类型描述
     */
    private final String text;
}
