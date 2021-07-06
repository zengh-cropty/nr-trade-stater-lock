package com.fantasi.lock.enums;

/**
 * 锁的模式
 *
 * @author zh
 */
public enum LockModel {
    //可重入锁
    REENTRANT,
    //公平锁（优先分配给先发出请求的线程）
    FAIR,
    //联锁(所有的锁都上锁成功才算成功，可以把一组锁当作一个锁来加锁和释放)
    MULTIPLE,
    //红锁（红锁在大部分节点上加锁成功就算成功。）
    RED_LOCK,
    //读锁（多个客户端可以同时加这个读锁，读锁和读锁是不互斥的）
    READ,
    //写锁
    WRITE,
    //自动模式,当参数只有一个.使用 REENTRANT 参数多个 RED_LOCK
    AUTO

    /**
     * 读锁与读锁非互斥
     * 读锁与写锁互斥
     * 写锁与写锁互斥
     * 读读、写写 同个客户端同个线程都可重入
     * 先写锁再加读锁可重入
     * 先读锁再写锁不可重入
     */
}
