package com.example.taskai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.taskai.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
        SELECT r.id, r.role_code, r.role_name, r.description, r.created_at, r.updated_at
        FROM sys_role r
        JOIN sys_user_role ur ON ur.role_id = r.id
        WHERE ur.user_id = #{userId}
        LIMIT 1
        """)
    SysRole selectRoleByUserId(Long userId);
}
