package cloud.marisa.picturebackend.exception;


import lombok.Getter;

/**
 * @author Marisa
 * @description 业务异常
 * @date 2025/03/25
 */
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BusinessException(ErrorCode error) {
        super(error.getMsg());
        this.code = error.getCode();
    }

    public BusinessException(ErrorCode error, String msg) {
        super(msg);
        this.code = error.getCode();
    }
}
