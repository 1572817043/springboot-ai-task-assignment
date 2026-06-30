package com.example.taskai.skill;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.skill.dto.SkillCreateRequest;
import com.example.taskai.skill.dto.SkillUpdateRequest;
import com.example.taskai.skill.service.SkillService;
import com.example.taskai.skill.vo.SkillDetailVO;
import com.example.taskai.skill.vo.SkillListItemVO;
import com.example.taskai.skill.vo.SkillOptionVO;
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
class SkillControllerTests {

    private static final String ADMIN_HEADER = "Bearer admin-token";
    private static final String MEMBER_HEADER = "Bearer member-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillService skillService;

    @MockitoBean
    private TokenService tokenService;

    private void mockAdminToken() {
        when(tokenService.parseToken("admin-token"))
                .thenReturn(new TokenService.TokenPayload("admin", "ADMIN"));
    }

    private void mockMemberToken() {
        when(tokenService.parseToken("member-token"))
                .thenReturn(new TokenService.TokenPayload("member", "MEMBER"));
    }

    @Test
    void listReturnsPagedSkills() throws Exception {
        mockMemberToken();

        SkillListItemVO item = new SkillListItemVO(
            1L, "Java", "后端", "Java 编程语言", LocalDateTime.now(), LocalDateTime.now()
        );
        Page<SkillListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of(item));
        page.setTotal(1);

        when(skillService.listSkills(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/skills").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].skillName").value("Java"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void detailReturnsSkill() throws Exception {
        mockMemberToken();

        SkillDetailVO detail = new SkillDetailVO(
            1L, "Java", "后端", "Java 编程语言", LocalDateTime.now(), LocalDateTime.now()
        );
        when(skillService.getSkillDetail(1L)).thenReturn(detail);

        mockMvc.perform(get("/api/skills/1").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.skillName").value("Java"));
    }

    @Test
    void detailReturns404ForMissingSkill() throws Exception {
        mockMemberToken();
        when(skillService.getSkillDetail(999L))
            .thenThrow(new BusinessException(404, "技能不存在", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/skills/999").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value(404))
            .andExpect(jsonPath("$.message").value("技能不存在"));
    }

    @Test
    void optionsReturnsSkillList() throws Exception {
        mockMemberToken();

        when(skillService.listOptions()).thenReturn(List.of(
            new SkillOptionVO(1L, "Java", "后端"),
            new SkillOptionVO(2L, "Vue", "前端")
        ));

        mockMvc.perform(get("/api/skills/options").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].skillName").value("Java"));
    }

    @Test
    void createReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(skillService).createSkill(any(SkillCreateRequest.class));

        mockMvc.perform(post("/api/skills")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skillName":"Docker","category":"运维","description":"容器化部署"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(skillService).createSkill(any(SkillCreateRequest.class));
    }

    @Test
    void createRejectsDuplicateName() throws Exception {
        mockAdminToken();
        doThrow(new BusinessException(400, "技能名称已存在", HttpStatus.BAD_REQUEST))
            .when(skillService).createSkill(any(SkillCreateRequest.class));

        mockMvc.perform(post("/api/skills")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skillName":"Java","category":"后端"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("技能名称已存在"));
    }

    @Test
    void createRejectsMemberRole() throws Exception {
        mockMemberToken();

        mockMvc.perform(post("/api/skills")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skillName":"Docker"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    void updateReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(skillService).updateSkill(eq(1L), any(SkillUpdateRequest.class));

        mockMvc.perform(put("/api/skills/1")
                .header("Authorization", ADMIN_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skillName":"Java 21","description":"Java LTS 版本"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(skillService).updateSkill(eq(1L), any(SkillUpdateRequest.class));
    }

    @Test
    void updateRejectsMemberRole() throws Exception {
        mockMemberToken();

        mockMvc.perform(put("/api/skills/1")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skillName":"Java 21"}
                    """))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void deleteReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(skillService).deleteSkill(1L);

        mockMvc.perform(delete("/api/skills/1").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(skillService).deleteSkill(1L);
    }

    @Test
    void deleteRejectsWhenSkillInUse() throws Exception {
        mockAdminToken();
        doThrow(new BusinessException(400, "该技能已被成员使用，无法删除", HttpStatus.BAD_REQUEST))
            .when(skillService).deleteSkill(1L);

        mockMvc.perform(delete("/api/skills/1").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("该技能已被成员使用，无法删除"));
    }

    @Test
    void deleteRejectsMemberRole() throws Exception {
        mockMemberToken();

        mockMvc.perform(delete("/api/skills/1").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void skillsEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/skills"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }
}
