package com.jianghu.winter.query.user;

import com.jianghu.winter.query.annotation.MapperTable;
import com.jianghu.winter.query.core.IMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author daniel.hu
 * @date 2019/8/27 10:19
 */
@Mapper
@MapperTable("t_user")
public interface UserMapper extends IMapper<UserEntity, Integer, UserQuery> {

}
