package cloud.marisa.picturebackend.exception;

/**
 * @author MarisaDAZE
 * @description 帮助抛出异常的工具类
 * @date 2025/3/25
 */
public class ThrowUtils {

    /**
     * 如果条件成立就抛出异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 如果条件成立就抛出异常
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param msg       提示信息
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String msg) {
        throwIf(condition, new BusinessException(errorCode, msg));
    }

    /**
     * 如果条件成立就抛出异常
     *
     * @param condition 条件
     * @param code      错误码
     * @param msg       提示信息
     */
    public static void throwIf(boolean condition, int code, String msg) {
        throwIf(condition, new BusinessException(code, msg));
    }


    /**
     * 如果条件成立就抛出异常
     *
     * @param condition 条件
     * @param err       异常类
     */
    public static void throwIf(boolean condition, RuntimeException err) {
        if (condition) {
            throw err;
        }
    }
}
