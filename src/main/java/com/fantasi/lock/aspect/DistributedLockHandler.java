package com.fantasi.lock.aspect;

import com.fantasi.lock.annotation.NLock;
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
     * @param nLock
     * @return Object
     */
    @SneakyThrows
    @Around("@annotation(nLock)")
    public Object around(ProceedingJoinPoint joinPoint, NLock nLock) {
        Object obj = null;
        log.debug("进入RedisLock环绕通知:{}...", nLock.lockKey());

        RLock rLock = getLock(joinPoint, nLock);
        boolean res = false;
        long expireSeconds = nLock.expireSeconds();
        long waitTime = nLock.waitTime();

        if (rLock != null) {
            try {
                if (waitTime == -1) {
                    res = true;
                    if (nLock.watchDogSwitch()) {
                        // 拿锁失败时会不停的重试; 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30s
                        rLock.lock();
                    } else {
                        // 一直等待加锁，上锁以后expireSeconds秒自动解锁; 不具备自动续期机制
                        rLock.lock(expireSeconds, TimeUnit.SECONDS);
                    }
                } else {
                    if (nLock.watchDogSwitch()) {
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
    private RLock getLock(ProceedingJoinPoint joinPoint, NLock nLock) {
        String[] keys = nLock.lockKey();
        if (keys.length == 0) {
            throw new RuntimeException("keys不能为空");
        }
        String[] parameterNames = new LocalVariableTableParameterNameDiscoverer().getParameterNames(((MethodSignature) joinPoint.getSignature()).getMethod());
        Object[] args = joinPoint.getArgs();

        LockModel lockModel = nLock.lockModel();
        RLock rLock = null;
        String keyConstant = nLock.keyPrefix();

        // 自动模式,当参数只有一个.使用 REENTRANT 参数多个 MULTIPLE
        if (lockModel.equals(LockModel.AUTO)) {
            if (keys.length > 1) {
                lockModel = LockModel.MULTIPLE;
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
