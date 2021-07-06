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
public @interface JLock {

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
     * key的静态常量:当key的spel的值是LIST,数组时使用+号连接将会被spel认为这个变量是个字符串
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
