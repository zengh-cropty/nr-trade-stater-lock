package com.fantasi.lock.test;

import com.fantasi.lock.annotation.NLock;
import com.fantasi.lock.annotation.NRepeat;
import com.fantasi.lock.client.RedissonLockClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Slf4j
public class LockService {

    @Resource
    private RedissonLockClient redissonLockClient;

    int n = 10;

    /**
     * 模拟秒杀(注解方式)
     */
    @NLock(lockKey = "#productId", expireSeconds = 10)
    public void seckill(String productId) {
        if (n <= 0) {
            log.info("注解方式--活动已结束,请下次再来");
            return;
        }
        log.info("注解方式--"+Thread.currentThread().getName() + ":秒杀到了商品， 当前剩余：" + --n);
    }

    /**
     * 模拟秒杀(编程方式)
     */
    public void seckill2(String productId) {
        boolean lockFlag = redissonLockClient.tryLock(productId, 2);
        if (lockFlag) {
            try {
                if (n <= 0) {
                    log.info("编程方式--活动已结束,请下次再来");
                    return;
                }
                log.info("编程方式--" + Thread.currentThread().getName() + ":秒杀到了商品, 剩余：" + --n);
            } finally {
                redissonLockClient.unlock(productId);
            }
        }
    }


    /**
     * 测试重复提交
     */
    @NRepeat(keyPrefix = "keyPrefix", lockKey = "#name", lockTime = 10)
    public void reSubmit(String name) {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            log.error("", e);
        }
        log.info("提交成功" + name);
    }
}
