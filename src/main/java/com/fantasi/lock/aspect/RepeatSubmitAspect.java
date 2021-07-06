package com.fantasi.lock.aspect;

import com.fantasi.lock.annotation.JRepeat;
import com.fantasi.lock.client.RedissonLockClient;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 防止重复提交分布式锁拦截器
 *
 * @author zh
 */
@Aspect
@Component
public class RepeatSubmitAspect extends BaseAspect {

    @Resource
    private RedissonLockClient redissonLockClient;

    /***
     * 定义controller切入点拦截规则，拦截JRepeat注解的业务方法
     */
    @Pointcut("@annotation(jRepeat)")
    public void pointCut(JRepeat jRepeat) {
    }

    /**
     * AOP分布式锁拦截
     *
     * @param joinPoint
     * @return
     * @throws Exception
     */
    @Around("pointCut(jRepeat)")
    public Object repeatSubmit(ProceedingJoinPoint joinPoint, JRepeat jRepeat) throws Throwable {
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());
        if (Objects.nonNull(jRepeat)) {
            Object[] args = joinPoint.getArgs();
            // 进行一些参数的处理，比如获取订单号，操作人id等
            String key = getValueBySpEL(jRepeat.lockKey(), parameterNames, args, "RepeatSubmit:" + jRepeat.keyPrefix() + ":").get(0);

            // 公平加锁，lockTime后锁自动释放
            boolean isLocked = false;
            try {
                isLocked = redissonLockClient.fairLock(key, TimeUnit.SECONDS, jRepeat.lockTime());
                // 如果成功获取到锁就继续执行
                if (isLocked) {
                    return joinPoint.proceed();
                } else {
                    throw new RuntimeException("请勿重复提交");
                }
            } finally {
                if (isLocked) {
                    redissonLockClient.unlock(key);
                }
            }
        }

        return joinPoint.proceed();
    }


}
