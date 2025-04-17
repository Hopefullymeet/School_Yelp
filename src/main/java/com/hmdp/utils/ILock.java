package com.hmdp.utils;

/**
 * @author lyrics61
 * @version 1.0
 * @since 2025
 */
public interface ILock {

    /**
     * 尝试获取锁，成功则返回True，失败则返回False
     * @param timeoutSec
     * @return
     */
    boolean tryLock(long timeoutSec);

    /**
     * 释放锁
     */
    void unlock();
}
