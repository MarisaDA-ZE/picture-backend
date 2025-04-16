package cloud.marisa.picturebackend.api.image.imageexpand.enums;

import cloud.marisa.picturebackend.enums.base.MrsBaseEnum;
import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 任务状态的枚举
 * @date 2025/4/10
 */
@Getter
public enum TaskStatusEnum implements MrsBaseEnum<String> {

    PENDING("PENDING", "任务排队中"),
    RUNNING("RUNNING", "任务处理中"),
    SUSPENDED("SUSPENDED", "任务挂起"),
    SUCCEEDED("SUCCEEDED", "任务执行成功"),
    FAILED("FAILED", "任务执行失败"),
    UNKNOWN("UNKNOWN", "任务不存在或状态未知");

    private final String value;
    private final String text;

    TaskStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }
}
