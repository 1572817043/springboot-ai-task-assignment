package com.example.taskai.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.taskai.project.mapper.ProjectMapper;
import com.example.taskai.project.mapper.ProjectMemberMapper;
import com.example.taskai.skill.mapper.SkillMapper;
import com.example.taskai.skill.mapper.TaskRequiredSkillMapper;
import com.example.taskai.task.dto.TaskAssignRequest;
import com.example.taskai.task.entity.Task;
import com.example.taskai.task.entity.TaskAssignment;
import com.example.taskai.task.entity.TaskStatusLog;
import com.example.taskai.task.mapper.TaskAssignmentMapper;
import com.example.taskai.task.mapper.TaskMapper;
import com.example.taskai.task.mapper.TaskResultMapper;
import com.example.taskai.task.mapper.TaskStatusLogMapper;
import com.example.taskai.task.service.TaskService;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysUserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TaskServiceTests {

    @Mock
    private TaskMapper taskMapper;
    @Mock
    private TaskAssignmentMapper taskAssignmentMapper;
    @Mock
    private TaskResultMapper taskResultMapper;
    @Mock
    private TaskStatusLogMapper taskStatusLogMapper;
    @Mock
    private TaskRequiredSkillMapper taskRequiredSkillMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProjectMemberMapper projectMemberMapper;
    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SkillMapper skillMapper;

    @InjectMocks
    private TaskService taskService;

    @Test
    void assignTaskFromUnassignedWritesCorrectStatusLog() {
        Task task = new Task();
        task.setId(1L);
        task.setProjectId(1L);
        task.setStatus("UNASSIGNED");
        task.setCreatorId(1L);

        SysUser assignee = new SysUser();
        assignee.setId(3L);

        when(taskMapper.selectById(1L)).thenReturn(task);
        when(sysUserMapper.selectById(3L)).thenReturn(assignee);
        when(projectMemberMapper.selectCount(any())).thenReturn(1L);

        taskService.assignTask(1L, new TaskAssignRequest(3L, "技能匹配"), "ADMIN", 1L);

        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskMapper).updateById(taskCaptor.capture());
        assertEquals("TODO", taskCaptor.getValue().getStatus());

        ArgumentCaptor<TaskStatusLog> logCaptor = ArgumentCaptor.forClass(TaskStatusLog.class);
        verify(taskStatusLogMapper).insert(logCaptor.capture());
        assertEquals("UNASSIGNED", logCaptor.getValue().getOldStatus());
        assertEquals("TODO", logCaptor.getValue().getNewStatus());
    }

    @Test
    void assignTaskByAiWritesSourceAi() {
        Task task = new Task();
        task.setId(1L);
        task.setProjectId(1L);
        task.setStatus("UNASSIGNED");
        task.setCreatorId(1L);

        when(taskMapper.selectById(1L)).thenReturn(task);

        taskService.assignTaskByAi(1L, 3L, 1L);

        ArgumentCaptor<TaskAssignment> assignmentCaptor = ArgumentCaptor.forClass(TaskAssignment.class);
        verify(taskAssignmentMapper).insert(assignmentCaptor.capture());
        assertEquals("AI", assignmentCaptor.getValue().getSource());
        assertEquals(3L, assignmentCaptor.getValue().getAssigneeId());
        assertEquals(1L, assignmentCaptor.getValue().getAssignedBy());
    }
}
