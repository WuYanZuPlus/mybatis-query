package com.jianghu.winter.query.user;

import com.jianghu.winter.query.core.QueryBuilder;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * @author daniel.hu
 * @date 2019/8/22 11:11
 */
public class QueryBuilderTest {

    private static final String ACCOUNT = "daniel";
    private QueryBuilder queryBuilder;

    @Before
    public void setUp() {
        queryBuilder = new QueryBuilder();
    }

    @Test
    public void buildSelect() {
        UserQuery build = UserQuery.builder().build();
        assertEquals("SELECT * FROM t_user", queryBuilder.buildSelect(build));
    }

    @Test
    public void buildCount() {
        UserQuery build = UserQuery.builder().build();
        assertEquals("SELECT COUNT(*) FROM t_user", queryBuilder.buildCount(build));
    }

    @Test
    public void where() {
        UserQuery byAccount = UserQuery.builder().account(ACCOUNT).build();
        String actualSql = queryBuilder.buildSelect(byAccount);
        assertEquals("SELECT * FROM t_user WHERE account = #{account}", actualSql);
    }

    @Test
    public void wheres() {
        UserQuery userQuery = UserQuery.builder().account(ACCOUNT).userName("赵子龙").build();
        assertEquals("SELECT * FROM t_user WHERE account = #{account} AND user_name = #{userName}", queryBuilder.buildSelect(userQuery));
    }

    @Test
    public void wheresAndPage() {
        UserQuery userQuery = UserQuery.builder().account(ACCOUNT).userName("赵子龙").build();
        userQuery.setPageNumber(0);
        assertEquals("SELECT * FROM t_user WHERE account = #{account} AND user_name = #{userName} LIMIT 0,10", queryBuilder.buildSelect(userQuery));
    }

    @Test
    public void sort() {
        UserQuery userQuery = UserQuery.builder().build();
        userQuery.setSort("user_name DESC");
        assertEquals("SELECT * FROM t_user ORDER BY user_name DESC", queryBuilder.buildSelect(userQuery));
    }

    @Test
    public void whereAndLike() {
        UserQuery userQuery = UserQuery.builder().build();
        userQuery.setUserNameLike("da");
        assertEquals("SELECT * FROM t_user WHERE user_name LIKE #{userNameLike}", queryBuilder.buildSelect(userQuery));
    }

    @Test
    public void whereAndIn() {
        UserQuery userQuery = UserQuery.builder().build();
        userQuery.setIdIn(Arrays.asList(1, 2, 3));
        assertEquals("SELECT * FROM t_user WHERE id IN (#{idIn[0]}, #{idIn[1]}, #{idIn[2]})", queryBuilder.buildSelect(userQuery));
    }

    @Test
    public void whereAndIn2() {
        UserQuery userQuery = UserQuery.builder().build();
        userQuery.setIdIn(new ArrayList<>());
        assertEquals("SELECT * FROM t_user", queryBuilder.buildSelect(userQuery));
    }

    @Test
    public void deleteAndWhere() {
        UserQuery userQuery = UserQuery.builder().account(ACCOUNT).build();
        assertEquals("DELETE FROM t_user WHERE account = #{account}", queryBuilder.buildDelete(userQuery));
    }


}