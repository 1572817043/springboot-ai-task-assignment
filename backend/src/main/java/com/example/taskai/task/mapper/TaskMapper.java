package com.example.taskai.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.vo.TaskListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {

    @Select("""
        <script>
        SELECT t.id, t.project_id AS projectId, p.project_name AS projectName,
               t.title, t.priority, t.status,
               t.creator_id AS creatorId, cu.real_name AS creatorName,
               t.assignee_id AS assigneeId, au.real_name AS assigneeName,
               t.deadline, t.estimated_hours AS estimatedHours,
               t.created_at AS createdAt, t.updated_at AS updatedAt
        FROM task t
        LEFT JOIN project p ON p.id = t.project_id
        LEFT JOIN sys_user cu ON cu.id = t.creator_id
        LEFT JOIN sys_user au ON au.id = t.assignee_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND t.title LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="projectId != null">
                AND t.project_id = #{projectId}
            </if>
            <if test="status != null and status != ''">
                AND t.status = #{status}
            </if>
            <if test="priority != null and priority != ''">
                AND t.priority = #{priority}
            </if>
            <if test="assigneeId != null">
                AND t.assignee_id = #{assigneeId}
            </if>
            <if test="projectIds != null and projectIds.size() > 0">
                AND t.project_id IN
                <foreach collection="projectIds" item="pid" open="(" separator="," close=")">
                    #{pid}
                </foreach>
            </if>
        </where>
        ORDER BY t.id DESC
        </script>
        """)
    IPage<TaskListItemVO> selectTaskPage(IPage<TaskListItemVO> page,
                                          @Param("keyword") String keyword,
                                          @Param("projectId") Long projectId,
                                          @Param("status") String status,
                                          @Param("priority") String priority,
                                          @Param("assigneeId") Long assigneeId,
                                          @Param("projectIds") java.util.List<Long> projectIds);
}
