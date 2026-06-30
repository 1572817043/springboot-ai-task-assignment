package com.example.taskai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.vo.UserListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("""
        <script>
        SELECT u.id, u.username, u.real_name AS realName, u.email, u.phone,
               u.status, r.role_code AS roleCode, r.role_name AS roleName, u.created_at AS createdAt
        FROM sys_user u
        LEFT JOIN sys_user_role ur ON ur.user_id = u.id
        LEFT JOIN sys_role r ON r.id = ur.role_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND (u.username LIKE CONCAT('%', #{keyword}, '%')
                     OR u.real_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="status != null and status != ''">
                AND u.status = #{status}
            </if>
        </where>
        ORDER BY u.id DESC
        </script>
        """)
    IPage<UserListItemVO> selectUsersWithRole(IPage<UserListItemVO> page,
                                               @Param("keyword") String keyword,
                                               @Param("status") String status);
}
