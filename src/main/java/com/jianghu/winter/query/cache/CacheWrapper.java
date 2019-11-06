package com.jianghu.winter.query.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.support.NoOpCache;

/**
 * @author daniel.hu
 */
@Slf4j
public class CacheWrapper {
    private CacheWrapper() {
    }

    @SuppressWarnings("unchecked")
    public static <V> V execute(Cache cache, Object key, CacheInvoker<V> cacheInvoker) {
        if (cache instanceof NoOpCache || key == null) {
            return cacheInvoker.invoke();
        }
        try {
            Cache.ValueWrapper valueWrapper = cache.get(key);
            if (valueWrapper != null) {
                return (V) valueWrapper.get();
            }
        } catch (Exception e) {
            log.error(String.format("Cache#get failed: [cache=%s, key=%s]", cache.getName(), key), e);
        }
        V value = cacheInvoker.invoke();
        cache.put(key, value);
        return value;
    }
}
