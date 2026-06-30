package com.example.taskai.member;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.member.dto.MemberProfileUpdateRequest;
import com.example.taskai.member.dto.MemberSkillItemRequest;
import com.example.taskai.member.dto.MemberSkillSaveRequest;
import com.example.taskai.member.service.MemberProfileService;
import com.example.taskai.member.vo.MemberProfileDetailVO;
import com.example.taskai.member.vo.MemberProfileListItemVO;
import com.example.taskai.member.vo.MemberSkillVO;
import java.math.BigDecimal;
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
class MemberProfileControllerTests {

    private static final String ADMIN_HEADER = "Bearer admin-token";
    private static final String MANAGER_HEADER = "Bearer manager-token";
    private static final String MEMBER_HEADER = "Bearer member-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberProfileService memberProfileService;

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
    void adminCanListProfiles() throws Exception {
        mockAdminToken();

        MemberProfileListItemVO item = new MemberProfileListItemVO(
            3L, "member", "成员用户", "member@example.com",
            2, 5, 1, new BigDecimal("80.00"), new BigDecimal("16.67"),
            List.of("Java", "Vue")
        );
        Page<MemberProfileListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of(item));
        page.setTotal(1);

        when(memberProfileService.listProfiles(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/member-profiles").header("Authorization", ADMIN_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].userId").value(3))
            .andExpect(jsonPath("$.data.records[0].skills[0]").value("Java"));
    }

    @Test
    void managerCanListProfiles() throws Exception {
        mockManagerToken();

        Page<MemberProfileListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);

        when(memberProfileService.listProfiles(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/member-profiles").header("Authorization", MANAGER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void memberCannotListProfiles() throws Exception {
        mockMemberToken();

        mockMvc.perform(get("/api/member-profiles").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    void memberCanViewOwnProfile() throws Exception {
        mockMemberToken();
        when(memberProfileService.resolveUserId("member")).thenReturn(3L);

        MemberProfileDetailVO detail = new MemberProfileDetailVO(
            3L, "member", "成员用户", "member@example.com", "13800000000",
            "简历内容", "经验总结", 2, 5, 1,
            new BigDecimal("80.00"), new BigDecimal("16.67"),
            List.of(new MemberSkillVO(1L, "Java", "后端", 3, new BigDecimal("2.0"), null))
        );
        when(memberProfileService.getProfileDetail(3L)).thenReturn(detail);

        mockMvc.perform(get("/api/member-profiles/3").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.username").value("member"))
            .andExpect(jsonPath("$.data.skills[0].skillName").value("Java"));
    }

    @Test
    void memberCannotViewOtherProfile() throws Exception {
        mockMemberToken();
        when(memberProfileService.resolveUserId("member")).thenReturn(3L);

        mockMvc.perform(get("/api/member-profiles/1").header("Authorization", MEMBER_HEADER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    void memberCanUpdateOwnProfile() throws Exception {
        mockMemberToken();
        when(memberProfileService.resolveUserId("member")).thenReturn(3L);
        doNothing().when(memberProfileService).updateProfile(eq(3L), any(MemberProfileUpdateRequest.class));

        mockMvc.perform(put("/api/member-profiles/3")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"resumeText":"新简历","currentWorkload":3}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(memberProfileService).updateProfile(eq(3L), any(MemberProfileUpdateRequest.class));
    }

    @Test
    void saveSkillsReturnsSuccess() throws Exception {
        mockMemberToken();
        when(memberProfileService.resolveUserId("member")).thenReturn(3L);
        doNothing().when(memberProfileService).saveSkills(eq(3L), any());

        mockMvc.perform(put("/api/member-profiles/3/skills")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skills":[{"skillId":1,"level":3,"years":2.0},{"skillId":2,"level":4,"years":1.5}]}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(memberProfileService).saveSkills(eq(3L), any());
    }

    @Test
    void saveSkillsRejectsInvalidLevel() throws Exception {
        mockMemberToken();
        when(memberProfileService.resolveUserId("member")).thenReturn(3L);

        mockMvc.perform(put("/api/member-profiles/3/skills")
                .header("Authorization", MEMBER_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"skills":[{"skillId":1,"level":6,"years":2.0}]}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void listRequiresToken() throws Exception {
        mockMvc.perform(get("/api/member-profiles"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }
}
