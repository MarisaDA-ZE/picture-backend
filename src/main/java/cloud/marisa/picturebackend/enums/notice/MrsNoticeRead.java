package cloud.marisa.picturebackend.enums.notice;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;


/**
 * @author MarisaDAZE
 * @description 消息读取状态枚举
 * @date 2025/4/20
 */
@Getter
public enum MrsNoticeRead implements MrsBaseEnum<Integer> {

    UNREAD(0, "未读"),
    READ(1, "已读");

    MrsNoticeRead(int value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 读取状态
     */
    private final Integer value;

    /**
     * 状态描述
     */
    private final String text;
}
