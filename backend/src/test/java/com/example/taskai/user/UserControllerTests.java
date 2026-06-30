package com.example.taskai.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.taskai.auth.service.TokenService;
import com.example.taskai.common.exception.BusinessException;
import com.example.taskai.user.dto.UserCreateRequest;
import com.example.taskai.user.dto.UserPasswordRequest;
import com.example.taskai.user.dto.UserStatusRequest;
import com.example.taskai.user.dto.UserUpdateRequest;
import com.example.taskai.user.service.UserService;
import com.example.taskai.user.vo.UserDetailVO;
import com.example.taskai.user.vo.UserListItemVO;
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
class UserControllerTests {

    private static final String AUTH_HEADER = "Bearer test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TokenService tokenService;

    private void mockAdminToken() {
        when(tokenService.parseToken("test-token"))
                .thenReturn(new TokenService.TokenPayload("admin", "ADMIN"));
    }

    @Test
    void listReturnsPagedUsers() throws Exception {
        mockAdminToken();

        UserListItemVO item = new UserListItemVO(
            1L, "admin", "管理员", "admin@example.com", null,
            "ENABLED", "ADMIN", "管理员", LocalDateTime.now()
        );
        Page<UserListItemVO> page = new Page<>(1, 10);
        page.setRecords(List.of(item));
        page.setTotal(1);

        when(userService.listUsers(null, null, 1, 10)).thenReturn(page);

        mockMvc.perform(get("/api/users").header("Authorization", AUTH_HEADER))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.records[0].username").value("admin"))
            .andExpect(jsonPath("$.data.records[0].roleCode").value("ADMIN"))
            .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void createReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(userService).createUser(any(UserCreateRequest.class));

        mockMvc.perform(post("/api/users")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"newuser","password":"123456","realName":"新用户","roleId":3}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    void createRejectsDuplicateUsername() throws Exception {
        mockAdminToken();
        doThrow(new BusinessException(400, "用户名已存在", HttpStatus.BAD_REQUEST))
            .when(userService).createUser(any(UserCreateRequest.class));

        mockMvc.perform(post("/api/users")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","password":"123456","realName":"重复用户","roleId":3}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(400))
            .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    void updateReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(userService).updateUser(eq(1L), any(UserUpdateRequest.class));

        mockMvc.perform(put("/api/users/1")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"realName":"新名字","email":"new@example.com"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(userService).updateUser(eq(1L), any(UserUpdateRequest.class));
    }

    @Test
    void updateStatusReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(userService).updateUserStatus(eq(1L), any(UserStatusRequest.class));

        mockMvc.perform(patch("/api/users/1/status")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status":"DISABLED"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(userService).updateUserStatus(eq(1L), any(UserStatusRequest.class));
    }

    @Test
    void resetPasswordReturnsSuccess() throws Exception {
        mockAdminToken();
        doNothing().when(userService).resetPassword(eq(1L), any(UserPasswordRequest.class));

        mockMvc.perform(patch("/api/users/1/password")
                .header("Authorization", AUTH_HEADER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"password":"newpass123"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));

        verify(userService).resetPassword(eq(1L), any(UserPasswordRequest.class));
    }
}
