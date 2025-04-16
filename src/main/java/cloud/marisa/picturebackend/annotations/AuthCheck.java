package cloud.marisa.picturebackend.annotations;

import cloud.marisa.picturebackend.enums.MrsUserRole;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author MarisaDAZE
 * @description 权限校验注解
 * @date 2025/3/28
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {

    /**
     * 必须具有的权限
     *
     * @return .
     */
    MrsUserRole mustRole() default MrsUserRole.GUEST;
}
