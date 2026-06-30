package com.example.taskai.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskai.auth.service.TokenService;
import com.example.taskai.user.entity.SysRole;
import com.example.taskai.user.mapper.SysRoleMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleControllerTests {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysRoleMapper sysRoleMapper;

    @MockitoBean
    private TokenService tokenService;

    private void mockAdminToken() {
        when(tokenService.parseToken("test-token"))
                .thenReturn(new TokenService.TokenPayload("admin", "ADMIN"));
    }

    @Test
    void optionsReturnsRoleList() throws Exception {
        mockAdminToken();

        SysRole admin = new SysRole();
        admin.setId(1L);
        admin.setRoleCode("ADMIN");
        admin.setRoleName("管理员");

        SysRole manager = new SysRole();
        manager.setId(2L);
        manager.setRoleCode("MANAGER");
        manager.setRoleName("项目经理");

        SysRole member = new SysRole();
        member.setId(3L);
        member.setRoleCode("MEMBER");
        member.setRoleName("成员");

        when(sysRoleMapper.selectList(any())).thenReturn(List.of(admin, manager, member));

        mockMvc.perform(get("/api/roles/options").header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].roleCode").value("ADMIN"))
            .andExpect(jsonPath("$.data[0].roleName").value("管理员"))
            .andExpect(jsonPath("$.data[1].roleCode").value("MANAGER"))
            .andExpect(jsonPath("$.data[2].roleCode").value("MEMBER"));
    }

    @Test
    void optionsRequiresToken() throws Exception {
        mockMvc.perform(get("/api/roles/options"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401));
    }
}
