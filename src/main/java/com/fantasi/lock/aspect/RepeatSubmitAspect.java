package com.fantasi.lock.aspect;

import com.fantasi.lock.annotation.NRepeat;
import com.fantasi.lock.client.RedissonLockClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
@Slf4j
@Component
public class RepeatSubmitAspect extends BaseAspect {

    @Resource
    private RedissonLockClient redissonLockClient;

    /***
     * 定义controller切入点拦截规则，拦截JRepeat注解的业务方法
     */
    @Pointcut("@annotation(nRepeat)")
    public void pointCut(NRepeat nRepeat) {
    }

    /**
     * AOP分布式锁拦截
     *
     * @param joinPoint
     * @return
     * @throws Exception
     */
    @Around("pointCut(nRepeat)")
    public Object repeatSubmit(ProceedingJoinPoint joinPoint, NRepeat nRepeat) throws Throwable {
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());
        if (Objects.nonNull(nRepeat)) {
            Object[] args = joinPoint.getArgs();
            // 进行一些参数的处理，比如获取订单号，操作人id等
            String keyConstant = "reSubmit:" + nRepeat.keyPrefix() + (StringUtils.isEmpty(nRepeat.keyPrefix()) ? "" : ":");
            String key = getValueBySpEL(nRepeat.lockKey(), parameterNames, args, keyConstant).get(0);

            // 公平加锁，lockTime后锁自动释放
            boolean isLocked = false;
            try {
                isLocked = redissonLockClient.fairLock(key, TimeUnit.SECONDS, nRepeat.lockTime());
                // 如果成功获取到锁就继续执行
                if (isLocked) {
                    return joinPoint.proceed();
                } else {
                    log.info(((MethodSignature) joinPoint.getSignature()).getMethod().getName() + ":请勿重复提交：" + key);
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
