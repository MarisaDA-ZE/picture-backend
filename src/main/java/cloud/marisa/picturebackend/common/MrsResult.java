package cloud.marisa.picturebackend.common;

import cloud.marisa.picturebackend.exception.ErrorCode;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * @author Marisa
 * @description 通用返回类
 * @date 2023/4/22
 */
@Data
@ToString
public class MrsResult<T> implements Serializable {
    private static final long serialVersion = 1L;

    private int code;
    private boolean status;
    private long timestamp;
    private String message;
    private T data;

    public MrsResult(int code, boolean status, String message) {
        this.code = code;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.message = message;
        this.data = null;
    }

    public MrsResult(int code, boolean status, String message, T data) {
        this.code = code;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
        this.message = message;
        this.data = data;
    }

    public MrsResult(int code, boolean status, long timestamp, String message, T data) {
        this.code = code;
        this.status = status;
        this.timestamp = timestamp;
        this.message = message;
        this.data = data;
    }

    public static <T> MrsResult<T> ok() {
        return new MrsResult<>(
                ErrorCode.SUCCESS.getCode(),
                true,
                ErrorCode.SUCCESS.getMsg());
    }

    public static <T> MrsResult<T> ok(String msg) {
        return new MrsResult<>(ErrorCode.SUCCESS.getCode(), true, msg);
    }

    public static <T> MrsResult<T> ok(T data) {
        return new MrsResult<>(ErrorCode.SUCCESS.getCode(), true, ErrorCode.SUCCESS.getMsg(), data);
    }

    public static <T> MrsResult<T> ok(String msg, T data) {
        return new MrsResult<>(ErrorCode.SUCCESS.getCode(), true, msg, data);
    }

    public static <T> MrsResult<T> ok(ErrorCode code, T data) {
        return new MrsResult<>(code.getCode(), true, code.getMsg(), data);
    }

    public static <T> MrsResult<T> ok(int code, String msg, T data) {
        return new MrsResult<>(code, true, msg, data);
    }

    public static <T> MrsResult<T> failed() {
        return new MrsResult<>(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                false,
                ErrorCode.INTERNAL_SERVER_ERROR.getMsg());
    }

    public static <T> MrsResult<T> failed(int code) {
        return new MrsResult<>(code, false, ErrorCode.INTERNAL_SERVER_ERROR.getMsg());
    }

    public static <T> MrsResult<T> failed(String msg) {
        return new MrsResult<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), false, msg);
    }

    public static <T> MrsResult<T> failed(T data) {
        return new MrsResult<>(ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                false,
                ErrorCode.INTERNAL_SERVER_ERROR.getMsg(),
                data);
    }

    public static <T> MrsResult<T> failed(int code, String msg) {
        return new MrsResult<>(code, false, msg);
    }

    public static <T> MrsResult<T> failed(int code, String msg, T data) {
        return new MrsResult<>(code, false, System.currentTimeMillis(), msg, data);
    }

    public static <T> MrsResult<T> failed(ErrorCode error) {
        return new MrsResult<>(error.getCode(), false, error.getMsg());
    }

    public static <T> MrsResult<T> failed(ErrorCode error, T data) {
        return new MrsResult<>(error.getCode(), false, error.getMsg(), data);
    }
}
