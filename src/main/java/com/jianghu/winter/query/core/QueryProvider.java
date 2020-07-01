package com.jianghu.winter.query.core;

import com.jianghu.winter.query.annotation.QueryField;
import com.jianghu.winter.query.annotation.QueryTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.persistence.Column;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author daniel.hu
 * @date 2019/8/22 10:47
 */
@Slf4j
public class QueryProvider {
    protected static final String LOG_SQL = "\nSQL: {}";
    private static final Map<String, Processor> suffixProcessorMap = new ConcurrentHashMap<>();

    static {
        suffixProcessorMap.put("defaultProcessor", new Processor.DefaultProcessor());
        suffixProcessorMap.put("inProcessor", new Processor.InProcessor());
        suffixProcessorMap.put("likeProcessor", new Processor.LikeProcessor());
    }

    public String buildSelect(Object query) {
        return build(query, Operation.SELECT);
    }

    public String buildCount(Object query) {
        return build(query, Operation.COUNT);
    }

    /**
     * 动态拼接查询sql
     */
    protected String build(Object query, Operation operation) {
        String selectSql = buildStartSql(query, operation);
        selectSql = buildWhereSql(selectSql, query);
        if (operation == Operation.SELECT && query instanceof PageQuery) {
            PageQuery pageQuery = (PageQuery) query;
            selectSql = buildSortSql(selectSql, pageQuery);
            selectSql = buildPageSql(selectSql, pageQuery);
        }
        log.debug(LOG_SQL, selectSql);
        return selectSql;
    }

    /**
     * 构建开始sql
     */
    private String buildStartSql(Object query, Operation operation) {
        QueryTable queryTable = query.getClass().getAnnotation(QueryTable.class);
        if (queryTable == null) {
            throw new IllegalStateException("@QueryTable annotation unConfigured!");
        }
        String startSql = "";
        switch (operation) {
            case SELECT:
                startSql = "SELECT " + buildSelectColumn(queryTable.entity());
                break;
            case COUNT:
                startSql = "SELECT COUNT(*)";
                break;
            case DELETE:
                startSql = "DELETE";
                break;
            default:
        }
        return startSql + " FROM " + queryTable.table();
    }

    /**
     * 优化查询，select *优化为 select实体类映射的字段
     */
    private String buildSelectColumn(Class<?> clazz) {
        List<String> needSelectColumn = Arrays.stream(FieldUtils.getAllFields(clazz))
                .filter(this::shouldRetain)
                .map(this::selectAs)
                .collect(Collectors.toList());
        return needSelectColumn.isEmpty() ? " *" : StringUtils.join(needSelectColumn, ", ");
    }

    /**
     * 构建条件查询sql
     */
    protected String buildWhereSql(String selectSql, Object query) {
        List<String> whereList = new LinkedList<>();
        Arrays.stream(query.getClass().getDeclaredFields()).forEach(field -> {
            Object fieldValue = readFieldValue(query, field);
            if (fieldValue == null) {
                return;
            }
            if (field.isAnnotationPresent(QueryField.class)) {
                String andSql = field.getAnnotation(QueryField.class).and();
                if (StringUtils.isNotBlank(andSql)) {
                    whereList.add(andSql);
                }
                return;
            }
            String fieldName = field.getName();
            QuerySuffixEnum suffixEnum = QuerySuffixEnum.resolve(fieldName);
            String columnName = CommonUtil.camelCaseToUnderscore(suffixEnum.resolveColumnName(fieldName));
            suffixProcessorMap.get(suffixEnum.name().toLowerCase() + Processor.class.getSimpleName())
                    .process(whereList, columnName, fieldName, fieldValue);
            if (suffixEnum == QuerySuffixEnum.Like) {
                reWriteFieldValue(query, fieldName, CommonUtil.reWriteLikeValue(fieldValue.toString()));
            }
        });
        if (!whereList.isEmpty()) {
            String whereSql = " WHERE " + StringUtils.join(whereList, " AND ");
            selectSql += whereSql;
        }
        return selectSql;
    }

    /**
     * 排序sql
     */
    private String buildSortSql(String selectSql, PageQuery pageQuery) {
        if (StringUtils.isNotBlank(pageQuery.getSort())) {
            selectSql += " ORDER BY " + pageQuery.getSort();
        }
        return selectSql;
    }

    /**
     * 分页sql
     */
    private String buildPageSql(String selectSql, PageQuery pageQuery) {
        if (pageQuery.needPaging()) {
            String pageSql = " LIMIT " + pageQuery.getOffset() + "," + pageQuery.getPageSize();
            selectSql += pageSql;
        }
        return selectSql;
    }

    protected Object readFieldValue(Object query, Field field) {
        try {
            return FieldUtils.readField(field, query, true);
        } catch (IllegalAccessException e) {
            log.error("Get the field value exception by reflection: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 重新赋值fieldValue
     */
    private void reWriteFieldValue(Object target, String fieldName, Object fieldValue) {
        try {
            FieldUtils.writeDeclaredField(target, fieldName, fieldValue, true);
        } catch (IllegalAccessException e) {
            log.error("Override exception for field value suffixed with like: {}", e.getMessage());
        }
    }

    private boolean shouldRetain(Field field) {
        return !field.getName().startsWith("$")
                && !Modifier.isStatic(field.getModifiers())
                && !field.isAnnotationPresent(Transient.class);
    }

    /**
     * 字段处理（先判断有无@Cloumn注解，然后驼峰处理）
     */
    public static String resolveColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        return column != null && !column.name().isEmpty() ? column.name() : CommonUtil.camelCaseToUnderscore(field.getName());
    }

    /**
     * 驼峰字段需别名处理
     * 应用场景：选择字段查询时（非select *）
     * @return 如: user_code AS userCode
     */
    private String selectAs(Field field) {
        String columnName = resolveColumnName(field);
        String fieldName = field.getName();
        return columnName.equalsIgnoreCase(fieldName) ? columnName : columnName + " AS " + fieldName;
    }
}
