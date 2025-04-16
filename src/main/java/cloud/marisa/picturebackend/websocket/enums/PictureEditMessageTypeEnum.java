package cloud.marisa.picturebackend.websocket.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum PictureEditMessageTypeEnum implements MrsBaseEnum<String> {
    INFO("INFO", "发送通知"),
    ERROR("ERROR", "发送错误"),
    ENTER_EDIT("ENTER_EDIT", "进入编辑状态"),
    EXIT_EDIT("EXIT_EDIT", "退出编辑状态"),
    EDIT_ACTION("EDIT_ACTION", "执行编辑操作");

    /**
     * 枚举值
     */
    private final String value;

    /**
     * 枚举描述
     */
    private final String text;

    PictureEditMessageTypeEnum(String value, String text) {
        this.text = text;
        this.value = value;
    }
}
