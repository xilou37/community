package com.lf.community.dao;

import com.lf.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * Description:
 *
 * @Author lf
 * @Create 2023/3/4 0004 19:04
 * @Version 1.0
 */
@Mapper
public interface UserMapper {
    
    User selectById(int id);
    
    int insertUser(User user);
}