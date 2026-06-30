package com.example.taskai.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.project.entity.Project;
import com.example.taskai.project.vo.ProjectListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("""
        <script>
        SELECT p.id, p.project_name AS projectName, p.description,
               p.manager_id AS managerId, u.real_name AS managerName,
               p.status, p.start_date AS startDate, p.end_date AS endDate,
               (SELECT COUNT(*) FROM project_member pm WHERE pm.project_id = p.id) AS memberCount,
               p.created_at AS createdAt, p.updated_at AS updatedAt
        FROM project p
        LEFT JOIN sys_user u ON u.id = p.manager_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND p.project_name LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="status != null and status != ''">
                AND p.status = #{status}
            </if>
            <if test="userId != null">
                AND (p.manager_id = #{userId}
                     OR EXISTS (SELECT 1 FROM project_member pm2 WHERE pm2.project_id = p.id AND pm2.user_id = #{userId}))
            </if>
        </where>
        ORDER BY p.id DESC
        </script>
        """)
    IPage<ProjectListItemVO> selectProjectPage(IPage<ProjectListItemVO> page,
                                                @Param("keyword") String keyword,
                                                @Param("status") String status,
                                                @Param("userId") Long userId);
}
