package com.jianghu.winter.query.cache;

/**
 * @author daniel.hu
 */
@FunctionalInterface
public interface CacheInvoker<T> {
    T invoke();
}
