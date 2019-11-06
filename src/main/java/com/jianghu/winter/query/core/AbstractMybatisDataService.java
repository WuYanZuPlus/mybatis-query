package com.jianghu.winter.query.core;

import com.jianghu.winter.query.cache.CacheUtil;
import com.jianghu.winter.query.cache.CacheWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCache;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author daniel.hu
 */
public abstract class AbstractMybatisDataService<E extends Persistable<I>, I extends Serializable, Q extends PageQuery> implements MybatisDataService<E, I, Q> {

    protected abstract IMapper<E, I, Q> getMapper();

    protected Cache cache = new NoOpCache("noOpCache");

    protected final Class<E> entityClass;

    @Autowired(required = false)
    public void setCacheManager(CacheManager cacheManager) {
        if (cacheManager != null) {
            cache = cacheManager.getCache(getCacheName());
        }
    }

    @SuppressWarnings("unchecked")
    public AbstractMybatisDataService() {
        this.entityClass = (Class<E>) getParameterizedType();
    }

    private Type getParameterizedType() {
        Type type = this.getClass().getGenericSuperclass();
        // 泛型数组
        Type[] parameterizedTypes = ((ParameterizedType) type).getActualTypeArguments();
        return parameterizedTypes[0];
    }

    protected String getCacheName() {
        return entityClass.getSimpleName().intern();
    }

    @Override
    public E get(I id) {
        return getMapper().get(id);
    }

    @Override
    public E get(Q query) {
        return CommonUtil.first(query(query));
    }

    @Override
    public void create(E entity) {
        try {
            getMapper().insert(entity);
        } finally {
            cache.clear();
        }
    }

    @Override
    public int create(Iterable<E> entities) {
        try {
            return getMapper().batchInsert(entities);
        } finally {
            cache.clear();
        }
    }

    @Override
    public void update(E entity) {
        try {
            getMapper().update(entity);
        } finally {
            cache.clear();
        }
    }

    @Override
    public void patch(E entity) {
        try {
            getMapper().patch(entity);
        } finally {
            cache.clear();
        }
    }

    @Override
    public int patch(E entity, Q query) {
        try {
            return getMapper().patchByQuery(entity, query);
        } finally {
            cache.clear();
        }
    }

    @Override
    public void delete(I id) {
        try {
            getMapper().delete(id);
        } finally {
            cache.clear();
        }
    }

    @Override
    public int delete(Q query) {
        try {
            return getMapper().deleteByQuery(query);
        } finally {
            cache.clear();
        }
    }

    @Override
    public List<E> query(Q query) {
        String queryKey = "query_" + CacheUtil.transformObjectToCacheKey(query);
        return CacheWrapper.execute(cache, queryKey, () -> getMapper().query(query));
    }

    @Override
    public long count(Q query) {
        String queryKey = "count_" + CacheUtil.transformObjectToCacheKey(query);
        return CacheWrapper.execute(cache, queryKey, () -> getMapper().count(query));
    }
}
