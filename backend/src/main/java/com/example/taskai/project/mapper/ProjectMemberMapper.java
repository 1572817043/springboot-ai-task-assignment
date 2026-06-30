package com.example.taskai.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.taskai.project.entity.ProjectMember;
import com.example.taskai.project.vo.ProjectMemberVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

    @Select("""
        SELECT pm.user_id AS userId, u.username, u.real_name AS realName,
               pm.project_role AS projectRole, pm.joined_at AS joinedAt
        FROM project_member pm
        INNER JOIN sys_user u ON u.id = pm.user_id
        WHERE pm.project_id = #{projectId}
        ORDER BY pm.id ASC
        """)
    List<ProjectMemberVO> selectMembersByProjectId(@Param("projectId") Long projectId);
}
