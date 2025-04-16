package cloud.marisa.picturebackend.websocket.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public enum PictureEditActionEnum implements MrsBaseEnum<String> {

    ZOOM_IN("ZOOM_IN", "放大操作"),
    ZOOM_OUT("ZOOM_OUT", "缩小操作"),
    ROTATE_LEFT("ROTATE_LEFT", "左旋操作"),
    ROTATE_RIGHT("ROTATE_RIGHT", "右旋操作");

    private final String text;
    private final String value;

    PictureEditActionEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
