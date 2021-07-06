package com.fantasi.lock.aspect;

import com.fantasi.lock.annotation.JLock;
import com.fantasi.lock.enums.LockModel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.RedissonMultiLock;
import org.redisson.RedissonRedLock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * 分布式锁解析器
 *
 * @author zh
 */
@Slf4j
@Aspect
@Component
public class DistributedLockHandler extends BaseAspect {


    @Autowired(required = false)
    private RedissonClient redissonClient;

    /**
     * 切面环绕通知
     *
     * @param joinPoint
     * @param jLock
     * @return Object
     */
    @SneakyThrows
    @Around("@annotation(jLock)")
    public Object around(ProceedingJoinPoint joinPoint, JLock jLock) {
        Object obj = null;
        log.debug("进入RedisLock环绕通知:{}...", jLock.lockKey());
        RLock rLock = getLock(joinPoint, jLock);
        boolean res = false;
        //获取超时时间
        long expireSeconds = jLock.expireSeconds();
        //等待多久,n秒内获取不到锁，则直接返回
        long waitTime = jLock.waitTime();
        //执行aop
        if (rLock != null) {
            try {
                if (waitTime == -1) {
                    res = true;
                    if (jLock.watchDogSwitch()) {
                        // 拿锁失败时会不停的重试; 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30s
                        rLock.lock();
                    } else {
                        // 一直等待加锁，上锁以后expireSeconds秒自动解锁; 不具备自动续期机制
                        rLock.lock(expireSeconds, TimeUnit.SECONDS);
                    }
                } else {
                    if (jLock.watchDogSwitch()) {
                        // 尝试拿锁waitTime秒后停止重试,返回false; 具有Watch Dog 自动延期机制 默认续30s
                        res = rLock.tryLock(waitTime, TimeUnit.SECONDS);
                    } else {
                        // 尝试拿锁waitTime秒后停止重试,返回false，上锁以后expireSeconds秒自动解锁; 不具备自动续期机制
                        res = rLock.tryLock(waitTime, expireSeconds, TimeUnit.SECONDS);
                    }
                }
                if (res) {
                    obj = joinPoint.proceed();
                } else {
                    log.error("获取锁失败");
                    throw new RuntimeException("系统错误，请重试");
                }
            } finally {
                if (res) {
                    rLock.unlock();
                }
            }
        }
        log.debug("结束RedisLock环绕通知...");
        return obj;
    }

    @SneakyThrows
    private RLock getLock(ProceedingJoinPoint joinPoint, JLock jLock) {
        String[] keys = jLock.lockKey();
        if (keys.length == 0) {
            throw new RuntimeException("keys不能为空");
        }
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());
        Object[] args = joinPoint.getArgs();

        LockModel lockModel = jLock.lockModel();
        if (!lockModel.equals(LockModel.MULTIPLE) && !lockModel.equals(LockModel.RED_LOCK) && keys.length > 1) {
            throw new RuntimeException("参数有多个,锁模式为->" + lockModel.name() + ".无法锁定");
        }
        RLock rLock = null;
        String keyConstant = jLock.keyPrefix();
        if (lockModel.equals(LockModel.AUTO)) {
            if (keys.length > 1) {
                lockModel = LockModel.RED_LOCK;
            } else {
                lockModel = LockModel.REENTRANT;
            }
        }
        switch (lockModel) {
            case FAIR:
                rLock = redissonClient.getFairLock(getValueBySpEL(keys[0], parameterNames, args, keyConstant).get(0));
                break;
            case RED_LOCK:
                List<RLock> rLocks = new ArrayList<>();
                for (String key : keys) {
                    List<String> valueBySpEL = getValueBySpEL(key, parameterNames, args, keyConstant);
                    for (String s : valueBySpEL) {
                        rLocks.add(redissonClient.getLock(s));
                    }
                }
                RLock[] locks = new RLock[rLocks.size()];
                int index = 0;
                for (RLock r : rLocks) {
                    locks[index++] = r;
                }
                rLock = new RedissonRedLock(locks);
                break;
            case MULTIPLE:
                rLocks = new ArrayList<>();

                for (String key : keys) {
                    List<String> valueBySpEL = getValueBySpEL(key, parameterNames, args, keyConstant);
                    for (String s : valueBySpEL) {
                        rLocks.add(redissonClient.getLock(s));
                    }
                }
                locks = new RLock[rLocks.size()];
                index = 0;
                for (RLock r : rLocks) {
                    locks[index++] = r;
                }
                rLock = new RedissonMultiLock(locks);
                break;
            case REENTRANT:
                List<String> valueBySpEL = getValueBySpEL(keys[0], parameterNames, args, keyConstant);
                //如果spel表达式是数组或者LIST 则使用红锁
                if (valueBySpEL.size() == 1) {
                    rLock = redissonClient.getLock(valueBySpEL.get(0));
                } else {
                    locks = new RLock[valueBySpEL.size()];
                    index = 0;
                    for (String s : valueBySpEL) {
                        locks[index++] = redissonClient.getLock(s);
                    }
                    rLock = new RedissonRedLock(locks);
                }
                break;
            case READ:
                rLock = redissonClient.getReadWriteLock(getValueBySpEL(keys[0], parameterNames, args, keyConstant).get(0)).readLock();
                break;
            case WRITE:
                rLock = redissonClient.getReadWriteLock(getValueBySpEL(keys[0], parameterNames, args, keyConstant).get(0)).writeLock();
                break;
        }
        return rLock;
    }
}
