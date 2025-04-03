package cloud.marisa.picturebackend.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author MarisaDAZE
 * @description 可重入锁注解标识
 * @date 2025/4/3
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MrsReentrantLock {
    /**
     * 锁的键
     */
    String value() default "";

    /**
     * 获取锁的最大等待时间（10秒）
     */
    long waitTime() default 10;

    /**
     * 超时自动释放（-1表示使用 看门狗 自动续期）
     * <p>其它则表示n秒后自动释放</p>
     */
    long leaseTime() default -1;
}
