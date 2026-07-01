package com.example.taskai.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.project.dto.ProjectCreateRequest;
import com.example.taskai.project.dto.ProjectMemberAddRequest;
import com.example.taskai.project.dto.ProjectStatusRequest;
import com.example.taskai.project.dto.ProjectUpdateRequest;
import com.example.taskai.project.service.ProjectService;
import com.example.taskai.project.vo.ProjectDetailVO;
import com.example.taskai.project.vo.ProjectListItemVO;
import com.example.taskai.project.vo.ProjectMemberVO;
import java.time.LocalDate;
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
class ProjectControllerTests {

    private static final String ADMIN_HEADER = "Bearer admin-token";
    private static final String MANAGER_HEADER = "Bearer manager-token";
    private static final String MEMBER_HEADER = "Bearer member-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

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
    void adminCanListProjects() throws Exception {
        mockAdminToken();

        ProjectListItemVO item = new ProjectListItemVO(
            1L, "测试项目", "项目描述", 2L, "项目经理",
            "NOT_STARTED", LocalDate.now(), LocalDate.now().plusMonths(3),
            3, LocalDateTime.now(), LocalDateTime.now()
        );
        Page<ProjectListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of(item));
        page.setTotal(1);

        when(projectService.resolveUserId("admin")).thenReturn(1L);
        when(projectService.listProjects(null, null, 1, 10, "ADMIN", 1L)).thenReturn(page);

        mockMvc.perform(get("/api/projects").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].projectName").value("测试项目"))
            .andExpect(jsonPath("$.data.records[0].memberCount").value(3));
    }

    @Test
    void memberCanListProjects() throws Exception {
        mockMemberToken();

        Page<ProjectListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(projectService.resolveUserId("member")).thenReturn(3L);
        when(projectService.listProjects(null, null, 1, 10, "MEMBER", 3L)).thenReturn(page);

        mockMvc.perform(get("/api/projects").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void managerCanCreateProject() throws Exception {
        mockManagerToken();
        when(projectService.resolveUserId("manager")).thenReturn(2L);
        doNothing().when(projectService).createProject(any(ProjectCreateRequest.class), eq("MANAGER"), eq(2L));

        mockMvc.perform(post("/api/projects")
                .header("Authorization", MANAGER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectName":"新项目","description":"描述","status":"NOT_STARTED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(projectService).createProject(any(ProjectCreateRequest.class), eq("MANAGER"), eq(2L));
    }

    @Test
    void memberCannotCreateProject() throws Exception {
        mockMemberToken();

        mockMvc.perform(post("/api/projects")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectName":"新项目"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限创建项目"));
    }

    @Test
    void detailReturnsProject() throws Exception {
        mockAdminToken();
        when(projectService.resolveUserId("admin")).thenReturn(1L);

        ProjectMemberVO member = new ProjectMemberVO(
            2L, "manager", "项目经理", "负责人", LocalDateTime.now()
        );
        ProjectDetailVO detail = new ProjectDetailVO(
            1L, "测试项目", "描述", 2L, "项目经理",
            "IN_PROGRESS", LocalDate.now(), LocalDate.now().plusMonths(3),
            LocalDateTime.now(), LocalDateTime.now(), List.of(member)
        );
        when(projectService.getProjectDetail(1L, "ADMIN", 1L)).thenReturn(detail);

        mockMvc.perform(get("/api/projects/1").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.projectName").value("测试项目"))
            .andExpect(jsonPath("$.data.members[0].username").value("manager"));
    }

    @Test
    void updateReturnsSuccess() throws Exception {
        mockAdminToken();
        when(projectService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(projectService).updateProject(eq(1L), any(ProjectUpdateRequest.class), eq("ADMIN"), eq(1L));

        mockMvc.perform(put("/api/projects/1")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"projectName":"更新后的项目","status":"IN_PROGRESS"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(projectService).updateProject(eq(1L), any(ProjectUpdateRequest.class), eq("ADMIN"), eq(1L));
    }

    @Test
    void updateStatusReturnsSuccess() throws Exception {
        mockManagerToken();
        when(projectService.resolveUserId("manager")).thenReturn(2L);
        doNothing().when(projectService).updateProjectStatus(eq(1L), eq("COMPLETED"), eq("MANAGER"), eq(2L));

        mockMvc.perform(patch("/api/projects/1/status")
                .header("Authorization", MANAGER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"COMPLETED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(projectService).updateProjectStatus(eq(1L), eq("COMPLETED"), eq("MANAGER"), eq(2L));
    }

    @Test
    void addMemberReturnsSuccess() throws Exception {
        mockAdminToken();
        when(projectService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(projectService).addMember(eq(1L), any(ProjectMemberAddRequest.class), eq("ADMIN"), eq(1L));

        mockMvc.perform(post("/api/projects/1/members")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":3,"projectRole":"开发"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(projectService).addMember(eq(1L), any(ProjectMemberAddRequest.class), eq("ADMIN"), eq(1L));
    }

    @Test
    void addDuplicateMemberReturns400() throws Exception {
        mockAdminToken();
        when(projectService.resolveUserId("admin")).thenReturn(1L);
        org.mockito.Mockito.doThrow(new BusinessException(400, "该用户已是项目成员", HttpStatus.BAD_REQUEST))
            .when(projectService).addMember(eq(1L), any(ProjectMemberAddRequest.class), eq("ADMIN"), eq(1L));

        mockMvc.perform(post("/api/projects/1/members")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId":3,"projectRole":"开发"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("该用户已是项目成员"));
    }

    @Test
    void removeMemberReturnsSuccess() throws Exception {
        mockAdminToken();
        when(projectService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(projectService).removeMember(eq(1L), eq(3L), eq("ADMIN"), eq(1L));

        mockMvc.perform(delete("/api/projects/1/members/3").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(projectService).removeMember(eq(1L), eq(3L), eq("ADMIN"), eq(1L));
    }

    @Test
    void listRequiresToken() throws Exception {
        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }
}
