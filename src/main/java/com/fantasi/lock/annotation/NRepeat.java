package com.fantasi.lock.annotation;

import java.lang.annotation.*;

/**
 * 防止重复提交的注解
 *
 * @author zh
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
public @interface NRepeat {

    /**
     * 超时时间 秒
     *
     * @return
     */
    int lockTime();

    /**
     * redis存放key的前缀
     *
     * @return
     */
    String keyPrefix() default "";


    /**
     * redis 锁key的
     *
     * @return redis 锁key
     */
    String lockKey() default "";

}
