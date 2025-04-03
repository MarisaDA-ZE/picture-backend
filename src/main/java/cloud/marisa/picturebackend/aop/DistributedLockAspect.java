package cloud.marisa.picturebackend.aop;

import cloud.marisa.picturebackend.annotations.MrsReentrantLock;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author MarisaDAZE
 * @description 基于redisson的可重入锁AOP切面类
 * @date 2025/4/3
 */
@Aspect
@Component
public class DistributedLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 分布式锁的AOP切面
     * <p>可重入锁</p>
     *
     * @param joinPoint     切入点
     * @param reentrantLock 切点P
     * @return args
     */
    @Around("@annotation(reentrantLock)")
    public Object reentrantLock(ProceedingJoinPoint joinPoint,
                                MrsReentrantLock reentrantLock)
            throws Throwable {
        String lockKey = resolveLockKey(joinPoint, reentrantLock.value());
        long waitTime = reentrantLock.waitTime();
        long leaseTime = reentrantLock.leaseTime();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("获取分布式锁失败");
            }
            return joinPoint.proceed();
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 解析SpEL表达式生成动态锁键
     *
     * @param joinPoint     接入点
     * @param keyExpression SpEL表达式
     * @return key
     */
    private String resolveLockKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Object[] args = joinPoint.getArgs();

        ExpressionParser parser = new SpelExpressionParser();
        Expression expression = parser.parseExpression(keyExpression);
        EvaluationContext context = new StandardEvaluationContext();

        String[] parameterNames = signature.getParameterNames();
        for (int i = 0; i < parameterNames.length; i++) {
            context.setVariable(parameterNames[i], args[i]);
        }
        return expression.getValue(context, String.class);
    }

}
