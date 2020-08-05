package com.jianghu.winter.query.core;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @param <E> - entity (like UserEntity)
 * @param <I> - the type of the identifier (like Long or Integer)
 * @param <Q> - query (like UserQuery)
 * @author daniel.hu
 */
public interface MybatisDataService<E extends Persistable<I>, I extends Serializable, Q extends PageQuery> {

    E get(I id);

    E get(Q query);

    void create(E entity);

    int create(Iterable<E> entities);

    void update(E entity);

    void patch(E entity);

    int patch(E entity, Q query);

    void delete(I id);

    int delete(Q query);

    List<E> query(Q query);

    default <V> List<V> query(Q query, Function<E, V> transfer) {
        return query(query).stream().map(transfer).collect(Collectors.toList());
    }

    long count(Q query);

    default PageList<E> page(Q query) {
        return new PageList<>(query(query), count(query));
    }

    default <V> PageList<V> page(Q query, Function<E, V> transfer) {
        return new PageList<>(query(query, transfer), count(query));
    }

}
