package com.jianghu.winter.query.service;

import com.jianghu.winter.query.annotation.QueryTable;
import com.jianghu.winter.query.core.PageQuery;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author daniel.hu
 * @date 2019/8/22 11:12
 */
@Getter
@Setter
@Builder
@QueryTable(table = "t_user")
public class UserQuery extends PageQuery {
    private String account;
    private String userName;
}
