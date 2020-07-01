package com.jianghu.winter.query.annotation;

import com.jianghu.winter.query.core.QueryProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author daniel.hu
 * @date 2019/07/01
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryTable {
    /**
     * 指定表名称
     */
    String table();

    /**
     * 指定Query对象对应的实体类
     * 用于{@link QueryProvider}中优化查询操作，select * 优化为 select columns
     */
    Class<?> entity();
}
