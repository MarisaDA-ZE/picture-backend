package cloud.marisa.picturebackend.exception;

import cloud.marisa.picturebackend.common.MrsResult;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author MarisaDAZE
 * @description 全局异常捕获
 * @date 2025/3/25
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获业务异常
     *
     * @param e 异常信息
     * @return 结果对象
     */
    @ExceptionHandler(BusinessException.class)
    public MrsResult<BusinessException> businessExceptionHandler(BusinessException e) {
        log.error("业务异常: ", e);
        return MrsResult.failed(e.getCode(), e.getMessage());
    }


    /**
     * 参数校验未通过异常
     *
     * @param e 异常信息
     * @return 结果对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public MrsResult<?> handleValidException(MethodArgumentNotValidException e) {
        log.error("参数校验异常: ", e);
        List<Map<String, String>> errors = getFieldErrors(e.getBindingResult());
        return MrsResult.failed(ErrorCode.PARAMS_ERROR, errors);
    }

    /**
     * 捕获运行时异常
     *
     * @param e 异常信息
     * @return 结果对象
     */
    @ExceptionHandler(RuntimeException.class)
    public MrsResult<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("运行时异常: ", e);
        return MrsResult.failed(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /**
     * Sa-Token-未登录
     *
     * @param e .
     * @return .
     */
    @ExceptionHandler(NotLoginException.class)
    public MrsResult<?> notLoginException(NotLoginException e) {
        log.error("NotLoginException", e);
        return MrsResult.failed(ErrorCode.NOT_LOGIN_ERROR, e.getMessage());
    }

    /**
     * Sa-Token-没有权限
     *
     * @param e .
     * @return .
     */
    @ExceptionHandler(NotPermissionException.class)
    public MrsResult<?> notPermissionExceptionHandler(NotPermissionException e) {
        log.error("NotPermissionException", e);
        return MrsResult.failed(ErrorCode.AUTHORIZATION_ERROR, e.getMessage());
    }


    /**
     * 解构字段错误信息
     *
     * @param binding 字段错误信息
     * @return 封装成k, v结构的错误信息列表
     */
    private List<Map<String, String>> getFieldErrors(BindingResult binding) {
        return binding.getFieldErrors()
                .stream()
                .map(field -> {
                    String key = field.getField();
                    String val = field.getDefaultMessage();
                    Map<String, String> res = new LinkedHashMap<>();
                    res.put("key", key);
                    res.put("value", val);
                    return res;
                }).collect(Collectors.toList());
    }

    @ExceptionHandler(AsyncRequestTimeoutException.class)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void handleAsyncTimeout(AsyncRequestTimeoutException ex) {
        // 静默处理SSE超时异常
        // log.error("SSE超时了 {}", ex.getMessage());
        // ex.printStackTrace();
    }
}
