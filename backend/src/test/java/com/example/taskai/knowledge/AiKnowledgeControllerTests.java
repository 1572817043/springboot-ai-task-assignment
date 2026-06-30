package com.example.taskai.knowledge;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.knowledge.dto.KnowledgeIndexedRequest;
import com.example.taskai.knowledge.service.AiKnowledgeService;
import com.example.taskai.knowledge.vo.KnowledgeDocumentVO;
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
class AiKnowledgeControllerTests {

    private static final String ADMIN_HEADER = "Bearer admin-token";
    private static final String MANAGER_HEADER = "Bearer manager-token";
    private static final String MEMBER_HEADER = "Bearer member-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiKnowledgeService aiKnowledgeService;

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
    void syncResumeReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(aiKnowledgeService).syncMemberResume(3L);

        mockMvc.perform(post("/api/ai-knowledge/sync/member/3/resume")
                .header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(aiKnowledgeService).syncMemberResume(3L);
    }

    @Test
    void memberCanSyncOwnResume() throws Exception {
        mockMemberToken();
        when(aiKnowledgeService.resolveUserId("member")).thenReturn(3L);
        doNothing().when(aiKnowledgeService).syncMemberResume(3L);

        mockMvc.perform(post("/api/ai-knowledge/sync/member/3/resume")
                .header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(aiKnowledgeService).syncMemberResume(3L);
    }

    @Test
    void memberCannotSyncOtherResume() throws Exception {
        mockMemberToken();
        when(aiKnowledgeService.resolveUserId("member")).thenReturn(3L);

        mockMvc.perform(post("/api/ai-knowledge/sync/member/1/resume")
                .header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("只能同步自己的简历"));
    }

    @Test
    void syncTaskResultReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(aiKnowledgeService).syncTaskResult(1L);

        mockMvc.perform(post("/api/ai-knowledge/sync/tasks/1/result")
                .header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(aiKnowledgeService).syncTaskResult(1L);
    }

    @Test
    void memberCannotSyncTaskResult() throws Exception {
        mockMemberToken();

        mockMvc.perform(post("/api/ai-knowledge/sync/tasks/1/result")
                .header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限操作"));
    }

    @Test
    void listReturnsDocuments() throws Exception {
        mockAdminToken();

        KnowledgeDocumentVO doc = new KnowledgeDocumentVO(
            1L, 3L, "RESUME", 3L, "成员用户 简历画像", "简历内容", 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        Page<KnowledgeDocumentVO> page = new Page<>(1, 10);
        page.setRecords(List.of(doc));
        page.setTotal(1);

        when(aiKnowledgeService.listDocuments(null, null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/ai-knowledge/documents").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].title").value("成员用户 简历画像"))
            .andExpect(jsonPath("$.data.records[0].sourceType").value("RESUME"));
    }

    @Test
    void detailReturnsDocument() throws Exception {
        mockAdminToken();

        KnowledgeDocumentVO doc = new KnowledgeDocumentVO(
            1L, 3L, "RESUME", 3L, "成员用户 简历画像", "简历内容", 0,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(aiKnowledgeService.getDocument(1L)).thenReturn(doc);

        mockMvc.perform(get("/api/ai-knowledge/documents/1").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.sourceType").value("RESUME"));
    }

    @Test
    void updateIndexedReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(aiKnowledgeService).updateIndexed(1L, 1);

        mockMvc.perform(patch("/api/ai-knowledge/documents/1/indexed")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"indexed":1}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(aiKnowledgeService).updateIndexed(1L, 1);
    }

    @Test
    void memberCannotUpdateIndexed() throws Exception {
        mockMemberToken();

        mockMvc.perform(patch("/api/ai-knowledge/documents/1/indexed")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"indexed":1}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void listRequiresToken() throws Exception {
        mockMvc.perform(get("/api/ai-knowledge/documents"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }
}
