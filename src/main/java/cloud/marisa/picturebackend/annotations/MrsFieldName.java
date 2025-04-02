package cloud.marisa.picturebackend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author MarisaDAZE
 * @description 用于标识哪些字段可以排序及排序的名字
 * <p>这个名字一般是前端传过来的</p>
 * @date 2025/3/31
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MrsFieldName {

    /**
     * 排序字段名
     */
    String value() default "";
}
