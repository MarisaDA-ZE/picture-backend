package cloud.marisa.picturebackend.aop;

import cloud.marisa.picturebackend.annotations.AuthCheck;
import cloud.marisa.picturebackend.entity.dao.User;
import cloud.marisa.picturebackend.enums.MrsUserRole;
import cloud.marisa.picturebackend.exception.BusinessException;
import cloud.marisa.picturebackend.exception.ErrorCode;
import cloud.marisa.picturebackend.service.IUserService;
import cloud.marisa.picturebackend.util.EnumUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author MarisaDAZE
 * @description 权限验证拦截器
 * @date 2025/3/28
 */
@Aspect
@Component
public class AuthInterceptor {

    @Autowired
    private IUserService userService;

    /**
     * 切面拦截器
     * 使用注解的方法上，只有登录用户拥有某个权限时才能访问
     *
     * @param joinPoint 接入点
     * @param authCheck 注解
     * @return .
     * @throws Throwable .
     */
    @Around("@annotation(authCheck)")
    public Object interceptorAround(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (ObjectUtils.isEmpty(requestAttributes)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        User loginUser = userService.getLoginUser(httpServletRequest);
        MrsUserRole currentRole = EnumUtil.fromValue(loginUser.getUserRole(), MrsUserRole.class);
        MrsUserRole mustRole = authCheck.mustRole();

        // 不要权限
        if (ObjectUtils.isEmpty(mustRole)) {
            return joinPoint.proceed();
        }

        // 用户没有任何权限
        if (ObjectUtils.isEmpty(currentRole)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
        }

        // 权限不足(current < must)
        if (currentRole.notThanRole(mustRole)) {
            throw new BusinessException(ErrorCode.AUTHORIZATION_ERROR);
        }

        // 校验通过，放行
        return joinPoint.proceed();
    }
}
