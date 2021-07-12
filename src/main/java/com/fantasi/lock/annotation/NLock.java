package com.fantasi.lock.annotation;

import com.fantasi.lock.enums.LockModel;

import java.lang.annotation.*;

/**
 * Redisson分布式锁注解
 *
 * @author zh
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface NLock {

    /**
     * 锁的模式:如果不设置,自动模式,当参数只有一个.使用 REENTRANT 参数多个 MULTIPLE
     */
    LockModel lockModel() default LockModel.AUTO;

    /**
     * 如果keys有多个,如果不设置,则使用 联锁
     *
     * @return
     */
    String[] lockKey() default {};

    /**
     * key的静态常量
     *
     * @return
     */
    String keyPrefix() default "";


    /**
     * 锁超时时间,默认30秒
     *
     * @return int
     */
    long expireSeconds() default 30L;

    /**
     * 是否使用看门狗机制，使用看门狗（默认续30s 每隔30/3=10 秒续到30s），手动设置过期时间将失效，将采用系统默认过期时间30秒
     * @return
     */
    boolean watchDogSwitch() default true;

    /**
     * 等待加锁超时时间,默认5秒 -1 则表示一直等待
     *
     * @return int
     */
    long waitTime() default 5L;

    /**
     * 未取到锁时提示信息
     *
     * @return
     */
    String failMsg() default "获取锁失败，请稍后重试";
}
