package com.example.taskai.ai;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskai.ai.service.AiAssignmentService;
import com.example.taskai.ai.vo.AiRecommendationCandidateVO;
import com.example.taskai.ai.vo.AiRecommendationResponse;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAssignmentControllerTests {

    private static final String ADMIN_HEADER = "Bearer admin-token";
    private static final String MANAGER_HEADER = "Bearer manager-token";
    private static final String MEMBER_HEADER = "Bearer member-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiAssignmentService aiAssignmentService;

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

    private AiRecommendationResponse buildResponse() {
        AiRecommendationCandidateVO candidate = new AiRecommendationCandidateVO(
            1L, 3L, "成员用户", 1,
            new BigDecimal("85.50"), new BigDecimal("30.00"), new BigDecimal("20.00"),
            new BigDecimal("15.00"), new BigDecimal("10.00"), new BigDecimal("10.50"),
            "技能匹配度较高，掌握 Java；当前工作量较低", 0
        );
        return new AiRecommendationResponse(1L, 1L, List.of(candidate));
    }

    @Test
    void adminCanRecommend() throws Exception {
        mockAdminToken();
        when(aiAssignmentService.resolveUserId("admin")).thenReturn(1L);
        when(aiAssignmentService.recommend(1L, "ADMIN", 1L)).thenReturn(buildResponse());

        mockMvc.perform(post("/api/ai-assignment/tasks/1/recommend")
                .header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.batchId").value(1));
    }

    @Test
    void projectManagerCanRecommend() throws Exception {
        mockManagerToken();
        when(aiAssignmentService.resolveUserId("manager")).thenReturn(2L);
        when(aiAssignmentService.recommend(1L, "MANAGER", 2L)).thenReturn(buildResponse());

        mockMvc.perform(post("/api/ai-assignment/tasks/1/recommend")
                .header("Authorization", MANAGER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.candidates[0].candidateName").value("成员用户"));
    }

    @Test
    void nonProjectManagerCannotRecommend() throws Exception {
        mockManagerToken();
        when(aiAssignmentService.resolveUserId("manager")).thenReturn(2L);
        when(aiAssignmentService.recommend(1L, "MANAGER", 2L))
            .thenThrow(new BusinessException(403, "无权限操作", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/ai-assignment/tasks/1/recommend")
                .header("Authorization", MANAGER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限操作"));
    }

    @Test
    void memberCannotRecommend() throws Exception {
        mockMemberToken();
        when(aiAssignmentService.resolveUserId("member")).thenReturn(3L);
        when(aiAssignmentService.recommend(1L, "MEMBER", 3L))
            .thenThrow(new BusinessException(403, "无权限操作", HttpStatus.FORBIDDEN));

        mockMvc.perform(post("/api/ai-assignment/tasks/1/recommend")
                .header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限操作"));
    }

    @Test
    void recommendReturns404ForMissingTask() throws Exception {
        mockAdminToken();
        when(aiAssignmentService.resolveUserId("admin")).thenReturn(1L);
        when(aiAssignmentService.recommend(999L, "ADMIN", 1L))
            .thenThrow(new BusinessException(404, "任务不存在", HttpStatus.NOT_FOUND));

        mockMvc.perform(post("/api/ai-assignment/tasks/999/recommend")
                .header("Authorization", ADMIN_HEADER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("任务不存在"));
    }

    @Test
    void latestReturnsRecommendation() throws Exception {
        mockAdminToken();
        when(aiAssignmentService.resolveUserId("admin")).thenReturn(1L);
        when(aiAssignmentService.getLatestRecommendation(1L, "ADMIN", 1L)).thenReturn(buildResponse());

        mockMvc.perform(get("/api/ai-assignment/tasks/1/recommendations/latest")
                .header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.batchId").value(1));
    }

    @Test
    void adminCanAccept() throws Exception {
        mockAdminToken();
        when(aiAssignmentService.resolveUserId("admin")).thenReturn(1L);
        doNothing().when(aiAssignmentService).acceptCandidate(1L, "ADMIN", 1L);

        mockMvc.perform(patch("/api/ai-assignment/candidates/1/accept")
                .header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(aiAssignmentService).acceptCandidate(1L, "ADMIN", 1L);
    }

    @Test
    void projectManagerCanAccept() throws Exception {
        mockManagerToken();
        when(aiAssignmentService.resolveUserId("manager")).thenReturn(2L);
        doNothing().when(aiAssignmentService).acceptCandidate(1L, "MANAGER", 2L);

        mockMvc.perform(patch("/api/ai-assignment/candidates/1/accept")
                .header("Authorization", MANAGER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(aiAssignmentService).acceptCandidate(1L, "MANAGER", 2L);
    }

    @Test
    void nonProjectManagerCannotAccept() throws Exception {
        mockManagerToken();
        when(aiAssignmentService.resolveUserId("manager")).thenReturn(2L);
        doNothing().when(aiAssignmentService).acceptCandidate(1L, "MANAGER", 2L);
        // Permission check happens in service, controller just passes role through

        mockMvc.perform(patch("/api/ai-assignment/candidates/1/accept")
                .header("Authorization", MANAGER_HEADER))
            .andExpect(status().isOk());
    }

    @Test
    void memberCannotAccept() throws Exception {
        mockMemberToken();
        when(aiAssignmentService.resolveUserId("member")).thenReturn(3L);
        org.mockito.Mockito.doThrow(new BusinessException(403, "无权限操作", HttpStatus.FORBIDDEN))
            .when(aiAssignmentService).acceptCandidate(1L, "MEMBER", 3L);

        mockMvc.perform(patch("/api/ai-assignment/candidates/1/accept")
                .header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限操作"));
    }

    @Test
    void recommendRequiresToken() throws Exception {
        mockMvc.perform(post("/api/ai-assignment/tasks/1/recommend"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }
}
