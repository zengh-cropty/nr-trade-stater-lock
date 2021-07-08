package com.fantasi.lock.enums;

import lombok.Getter;

/**
 * 分布式锁枚举类
 * @author zh
 */
@Getter
public enum LockConstant {
    /**
     * 通用锁常量
     */
    COMMON("commonLock:", 1, 500, "请勿重复点击");
    /**
     * 分布式锁前缀
     */
    private String keyPrefix;
    /**
     * 等到最大时间，强制获取锁
     */
    private int waitTime;
    /**
     * 锁失效时间
     */
    private int leaseTime;
    /**
     * 加锁提示
     */
    private String message;

    LockConstant(String keyPrefix, int waitTime, int leaseTime, String message) {
        this.keyPrefix = keyPrefix;
        this.waitTime = waitTime;
        this.leaseTime = leaseTime;
        this.message = message;
    }
}
