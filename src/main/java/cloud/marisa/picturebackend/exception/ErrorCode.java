package cloud.marisa.picturebackend.exception;

import lombok.Getter;

/**
 * @author MarisaDAZE
 * @description 错误码
 * @date 2025/3/25
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "操作成功"),
    PARAMS_ERROR(40000, "参数错误"),
    NOT_LOGIN_ERROR(40100, "未登录的访问"),
    AUTHORIZATION_ERROR(40101, "未授权的访问"),
    FORBIDDEN_ERROR(40300, "无访问权限"),
    NOT_FOUND(40400, "资源不存在"),
    INTERNAL_SERVER_ERROR(50000, "服务器内部错误"),
    OPERATION_ERROR(50001, "操作失败");

    private final int code;
    private final String msg;

    ErrorCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
