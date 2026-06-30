package com.example.taskai.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.user.entity.SysRole;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysRoleMapper;
import com.example.taskai.user.mapper.SysUserMapper;
import com.example.taskai.user.service.UserService;
import com.example.taskai.user.vo.UserListItemVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthInterceptorTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenService tokenService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private SysUserMapper sysUserMapper;

    @MockitoBean
    private SysRoleMapper sysRoleMapper;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void rejectsMemberAccessingUsersEndpoint() throws Exception {
        when(tokenService.parseToken("member-token"))
                .thenReturn(new TokenService.TokenPayload("member", "MEMBER"));

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer member-token"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value(403))
            .andExpect(jsonPath("$.message").value("无权限访问"));
    }

    @Test
    void allowsAdminAccessingUsersEndpoint() throws Exception {
        when(tokenService.parseToken("admin-token"))
                .thenReturn(new TokenService.TokenPayload("admin", "ADMIN"));

        Page<UserListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(userService.listUsers(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer admin-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void rejectsRequestWithInvalidToken() throws Exception {
        when(tokenService.parseToken("bad-token"))
                .thenThrow(new com.example.taskai.common.exception.BusinessException(
                        401, "未登录或登录已过期", org.springframework.http.HttpStatus.UNAUTHORIZED));

        mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer bad-token"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void loginEndpointIsPublic() throws Exception {
        // Login endpoint is excluded from interceptor - should reach controller
        // (returns 401 from controller, not interceptor block)
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"nouser","password":"wrong"}
                    """))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("账号或密码错误"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/system/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void meEndpointRequiresToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value(401))
            .andExpect(jsonPath("$.message").value("未登录或登录已过期"));
    }

    @Test
    void meEndpointAllowsAnyRole() throws Exception {
        when(tokenService.parseToken("member-token"))
                .thenReturn(new TokenService.TokenPayload("member", "MEMBER"));

        SysUser user = new SysUser();
        user.setId(3L);
        user.setUsername("member");
        user.setRealName("成员用户");
        user.setStatus("ENABLED");
        when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        SysRole role = new SysRole();
        role.setRoleCode("MEMBER");
        when(sysRoleMapper.selectRoleByUserId(3L)).thenReturn(role);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer member-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }
}
