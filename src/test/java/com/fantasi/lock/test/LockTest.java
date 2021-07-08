package com.fantasi.lock.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LockTestApplication.class)
public class LockTest {
    @Autowired
    LockService lockService;

    @Autowired(required = false)
    private RedissonClient redissonClient;

    private void redissonDoc() throws InterruptedException {
        RLock lock = redissonClient.getLock("generalLock");
        // 拿锁失败时会不停的重试
        // 具有Watch Dog 自动延期机制 默认续30s 每隔30/3=10 秒续到30s
        lock.lock();
        // 尝试拿锁10s后停止重试,返回false
        // 具有Watch Dog 自动延期机制 默认续30s
        boolean res1 = lock.tryLock(10, TimeUnit.SECONDS);
        // 拿锁失败时会不停的重试
        // 没有Watch Dog ，10s后自动释放
        lock.lock(10, TimeUnit.SECONDS);
        // 尝试拿锁100s后停止重试,返回false
        // 没有Watch Dog ，10s后自动释放
        boolean res2 = lock.tryLock(100, 10, TimeUnit.SECONDS);
        //2. 公平锁 保证 Redisson 客户端线程将以其请求的顺序获得锁
        RLock fairLock = redissonClient.getFairLock("fairLock");
        //3. 读写锁 没错与JDK中ReentrantLock的读写锁效果一样
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("readWriteLock");
        readWriteLock.readLock().lock();
        readWriteLock.writeLock().lock();
    }

    /**
     * 测试分布式锁(模拟秒杀)
     */
    @Test
    public void test1() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(30);
        IntStream.range(0, 1000).forEach(i -> executorService.submit(() -> {
            try {
                lockService.seckill("20120508784");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * 测试分布式锁(模拟秒杀)
     */
    @Test
    public void test2() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        IntStream.range(0, 30).forEach(i -> executorService.submit(() -> {
            try {
                lockService.seckill2("20120508784");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        executorService.awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * 测试分布式锁(模拟重复提交)
     */
    @Test
    public void test3() throws Exception {
        ExecutorService executorService = Executors.newFixedThreadPool(6);
        IntStream.range(0, 20).forEach(i -> executorService.submit(() -> {
            try {
                lockService.reSubmit("test");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
        executorService.awaitTermination(15, TimeUnit.SECONDS);
    }
}
