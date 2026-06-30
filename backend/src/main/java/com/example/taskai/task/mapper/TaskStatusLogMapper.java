package com.example.taskai.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.taskai.task.entity.TaskStatusLog;
import com.example.taskai.task.vo.TaskStatusLogVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TaskStatusLogMapper extends BaseMapper<TaskStatusLog> {

    @Select("""
        SELECT tsl.old_status AS oldStatus, tsl.new_status AS newStatus,
               u.real_name AS operatorName, tsl.remark, tsl.created_at AS createdAt
        FROM task_status_log tsl
        LEFT JOIN sys_user u ON u.id = tsl.operator_id
        WHERE tsl.task_id = #{taskId}
        ORDER BY tsl.id ASC
        """)
    List<TaskStatusLogVO> selectLogsByTaskId(@Param("taskId") Long taskId);
}
