package com.jianghu.winter.query.core;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.persistence.GeneratedValue;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author daniel.hu
 * @date 2019/8/30 15:22
 */
@Slf4j
public class CrudProvider extends QueryProvider {
    private final Map<Class<?>, String> insertSqlMap = new HashMap<>();

    /**
     * delete
     */
    public String buildDelete(Object query) {
        return super.build(query, Operation.DELETE);
    }

    /**
     * insert
     */
    public String buildInsert(Object entity) {
        String insertSql = insertSqlMap.computeIfAbsent(entity.getClass(), entityClass -> buildInsert(entityClass));
        log.debug(LOG_SQL, insertSql);
        return insertSql;
    }

    /**
     * batch insert
     */
    public static String buildBatchInsert(Map map) {
        List<Object> entities = (List<Object>) map.get("list");
        Class<?> clazz0 = entities.get(0).getClass();
        List<Field> filteredFields = getFilteredFields(clazz0);
        List<String> columnNames = getInsertColumnNames(filteredFields);
        List<String> batchFieldValues = getBatchInsertFieldValues(filteredFields, entities.size());
        return buildInsertSql(resolveTableName(clazz0), columnNames, StringUtils.join(batchFieldValues, ", "));
    }

    private static String buildInsert(Class<?> entityClass) {
        List<Field> filteredFields = getFilteredFields(entityClass);
        List<String> columnNames = getInsertColumnNames(filteredFields);
        List<String> fieldValues = getInsertFieldValues(filteredFields);
        return buildInsertSql(resolveTableName(entityClass), columnNames, "(" + StringUtils.join(fieldValues, ", ") + ")");
    }

    private static String buildInsertSql(String tableName, List<String> columnNames, String fieldValues) {
        ArrayList<String> insertList = new ArrayList<>();
        insertList.add("INSERT INTO");
        insertList.add(tableName);
        insertList.add("(" + StringUtils.join(columnNames, ", ") + ")");
        insertList.add("VALUES");
        insertList.add(fieldValues);
        return StringUtils.join(insertList, " ");
    }

    /**
     * update
     */
    public String buildUpdate(Object entity) {
        String updateSql = buildUpdateSql(entity, Operation.UPDATE) + " WHERE id = #{id}";
        log.debug(LOG_SQL, updateSql);
        return updateSql;
    }

    /**
     * patch
     */
    public String buildPatch(Object entity) {
        String updateSql = buildUpdateSql(entity, Operation.PATCH) + " WHERE id = #{id}";
        log.debug(LOG_SQL, updateSql);
        return updateSql;
    }

    /**
     * patch by query
     * notice: When you have many parameters, use param1,param2... in order to avoid exceptions
     */
    public String buildPatchByQuery(Object entity, Object query) {
        String updateSql = buildUpdateSql(entity, Operation.PATCH);
        updateSql = RegExUtils.replaceAll(updateSql, "#\\{", "#{param1.");

        String whereSql = super.buildWhereSql("", query);
        whereSql = RegExUtils.replaceAll(whereSql, "#\\{", "#{param2.");
        log.debug(LOG_SQL, updateSql + whereSql);
        return updateSql + whereSql;
    }

    private String buildUpdateSql(Object entity, Operation operation) {
        ArrayList<String> updateList = new ArrayList<>();
        updateList.add("UPDATE");
        updateList.add(resolveTableName(entity.getClass()));
        updateList.add("SET");
        updateList.add(buildUpdateOrPatchFields(entity, operation));
        return StringUtils.join(updateList, " ");
    }

    private String buildUpdateOrPatchFields(Object entity, Operation operation) {
        List<Field> filteredFields = getFilteredFields(entity.getClass());
        List<String> updateFields = new LinkedList<>();
        switch (operation) {
            case UPDATE:
                for (Field field : filteredFields) {
                    updateFields.add(resolveColumnName(field) + " = " + "#{" + field.getName() + "}");
                }
                break;
            case PATCH:
                for (Field field : filteredFields) {
                    Object value = readFieldValue(entity, field);
                    if (value != null) {
                        updateFields.add(resolveColumnName(field) + " = " + "#{" + field.getName() + "}");
                    }
                }
                break;
            default:
        }
        return StringUtils.join(updateFields, ", ");
    }

    private static List<Field> getFilteredFields(Class<?> entityClass) {
        List<Field> allFields = FieldUtils.getAllFieldsList(entityClass);
        return allFields.stream().filter(field -> !isIgnoredField(field)).collect(Collectors.toList());
    }

    private static List<String> getInsertColumnNames(List<Field> filteredFields) {
        return filteredFields.stream().map(CrudProvider::resolveColumnName).collect(Collectors.toList());
    }

    private static List<String> getInsertFieldValues(List<Field> filteredFields) {
        return filteredFields.stream().map(field -> "#{" + field.getName() + "}").collect(Collectors.toList());
    }

    private static List<String> getBatchInsertFieldValues(List<Field> filteredFields, int size) {
        List<String> batchFieldValues = new ArrayList<>();
        List<String> fieldValues = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            for (Field field : filteredFields) {
                String format = String.format("#{list[%d].%s}", i, field.getName());
                fieldValues.add(format);
            }
            batchFieldValues.add("(" + StringUtils.join(fieldValues, ", ") + ")");
            fieldValues.clear();
        }
        return batchFieldValues;
    }

    private static boolean isIgnoredField(Field field) {
        return Modifier.isStatic(field.getModifiers())
                || field.isAnnotationPresent(GeneratedValue.class)
                || field.isAnnotationPresent(Transient.class);
    }

    private static String resolveTableName(Class<?> clazz) {
        Table table = clazz.getDeclaredAnnotation(Table.class);
        if (table == null) {
            throw new IllegalStateException("@Table annotation unConfigured!");
        }
        return table.name();
    }

}
