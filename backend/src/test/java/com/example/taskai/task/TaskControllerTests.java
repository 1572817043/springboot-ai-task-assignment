package com.example.taskai.task;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.task.dto.TaskAssignRequest;
import com.example.taskai.task.dto.TaskCreateRequest;
import com.example.taskai.task.dto.TaskResultReviewRequest;
import com.example.taskai.task.dto.TaskResultSubmitRequest;
import com.example.taskai.task.dto.TaskStatusRequest;
import com.example.taskai.task.dto.TaskUpdateRequest;
import com.example.taskai.task.service.TaskService;
import com.example.taskai.task.vo.TaskDetailVO;
import com.example.taskai.task.vo.TaskListItemVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerTests {

    private static final String ADMIN_HEADER = "Bearer admin-token";
    private static final String MANAGER_HEADER = "Bearer manager-token";
    private static final String MEMBER_HEADER = "Bearer member-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private TokenService tokenService;

    private void mockAdminToken() {
        when(tokenService.parseToken("admin-token"))
                .thenReturn(new TokenService.TokenPayload("admin", "ADMIN"));
    }

    private void mockManagerToken() {
        when(tokenService.parseToken("manager-token"))
                .thenReturn(new TokenService.TokenPayload("manager", "MANAGER"));
    }

    private void mockMemberToken() {
        when(tokenService.parseToken("member-token"))
                .thenReturn(new TokenService.TokenPayload("member", "MEMBER"));
    }

    @Test
    void adminCanListTasks() throws Exception {
        mockAdminToken();

        TaskListItemVO item = new TaskListItemVO(
            1L, 1L, "测试项目", "开发登录功能", "HIGH", "IN_PROGRESS",
            1L, "管理员", 3L, "成员用户",
            LocalDateTime.now().plusDays(7), new BigDecimal("16.00"),
            LocalDateTime.now(), LocalDateTime.now()
        );
        Page<TaskListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of(item));
        page.setTotal(1);

        when(taskService.resolveUserId("admin")).thenReturn(1L);
        when(taskService.listTasks(null, null, null, null, null, 1, 10, "ADMIN", 1L)).thenReturn(page);

        mockMvc.perform(get("/api/tasks").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].title").value("开发登录功能"))
            .andExpect(jsonPath("$.data.records[0].assigneeName").value("成员用户"));
    }

    @Test
    void memberCanListOwnTasks() throws Exception {
        mockMemberToken();

        Page<TaskListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(taskService.resolveUserId("member")).thenReturn(3L);
        when(taskService.listTasks(null, null, null, null, null, 1, 10, "MEMBER", 3L)).thenReturn(page);

        mockMvc.perform(get("/api/tasks").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void managerCanCreateTask() throws Exception {
        mockManagerToken();
        when(taskService.resolveUserId("manager")).thenReturn(2L);
        doNothing().when(taskService).createTask(any(TaskCreateRequest.class), eq(2L));

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", MANAGER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectId":1,"title":"新任务","description":"描述","priority":"HIGH"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(taskService).createTask(any(TaskCreateRequest.class), eq(2L));
    }

    @Test
    void memberCannotCreateTask() throws Exception {
        mockMemberToken();

        mockMvc.perform(post("/api/tasks")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectId":1,"title":"新任务"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限创建任务"));
    }

    @Test
    void detailReturnsTask() throws Exception {
        mockAdminToken();
        when(taskService.resolveUserId("admin")).thenReturn(1L);

        TaskDetailVO detail = new TaskDetailVO(
            1L, 1L, "测试项目", "开发登录功能", "详细描述", "HIGH", "IN_PROGRESS",
            1L, "管理员", 3L, "成员用户",
            LocalDateTime.now().plusDays(7), new BigDecimal("16.00"),
            LocalDateTime.now(), LocalDateTime.now(),
            List.of(), List.of(), null
        );
        when(taskService.getTaskDetail(1L, "ADMIN", 1L)).thenReturn(detail);

        mockMvc.perform(get("/api/tasks/1").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.title").value("开发登录功能"))
            .andExpect(jsonPath("$.data.projectName").value("测试项目"));
    }

    @Test
    void updateReturnsSuccess() throws Exception {
        mockAdminToken();
        when(taskService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(taskService).updateTask(eq(1L), any(TaskUpdateRequest.class), eq("ADMIN"), eq(1L));

        mockMvc.perform(put("/api/tasks/1")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"更新后的任务","priority":"MEDIUM"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(taskService).updateTask(eq(1L), any(TaskUpdateRequest.class), eq("ADMIN"), eq(1L));
    }

    @Test
    void updateStatusReturnsSuccess() throws Exception {
        mockManagerToken();
        when(taskService.resolveUserId("manager")).thenReturn(2L);
        doNothing().when(taskService).updateTaskStatus(eq(1L), eq("IN_PROGRESS"), eq("开始处理"), eq("MANAGER"), eq(2L));

        mockMvc.perform(patch("/api/tasks/1/status")
                .header("Authorization", MANAGER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"IN_PROGRESS","remark":"开始处理"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(taskService).updateTaskStatus(eq(1L), eq("IN_PROGRESS"), eq("开始处理"), eq("MANAGER"), eq(2L));
    }

    @Test
    void assignReturnsSuccess() throws Exception {
        mockAdminToken();
        when(taskService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(taskService).assignTask(eq(1L), any(TaskAssignRequest.class), eq("ADMIN"), eq(1L));

        mockMvc.perform(patch("/api/tasks/1/assignee")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assigneeId":3,"reason":"技能匹配"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(taskService).assignTask(eq(1L), any(TaskAssignRequest.class), eq("ADMIN"), eq(1L));
    }

    @Test
    void memberCannotAssign() throws Exception {
        mockMemberToken();

        mockMvc.perform(patch("/api/tasks/1/assignee")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assigneeId":3}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限分配任务"));
    }

    @Test
    void submitResultReturnsSuccess() throws Exception {
        mockMemberToken();
        when(taskService.resolveUserId("member")).thenReturn(3L);
        doNothing().when(taskService).submitResult(eq(1L), any(TaskResultSubmitRequest.class), eq("MEMBER"), eq(3L));

        mockMvc.perform(post("/api/tasks/1/result")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"resultSummary":"已完成开发","resultUrl":"http://example.com/result"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(taskService).submitResult(eq(1L), any(TaskResultSubmitRequest.class), eq("MEMBER"), eq(3L));
    }

    @Test
    void reviewResultReturnsSuccess() throws Exception {
        mockAdminToken();
        when(taskService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(taskService).reviewResult(eq(1L), any(TaskResultReviewRequest.class), eq("ADMIN"), eq(1L));

        mockMvc.perform(patch("/api/tasks/1/result/review")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"reviewStatus":"APPROVED","reviewComment":"质量不错"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(taskService).reviewResult(eq(1L), any(TaskResultReviewRequest.class), eq("ADMIN"), eq(1L));
    }

    @Test
    void listRequiresToken() throws Exception {
        mockMvc.perform(get("/api/tasks"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }
}
