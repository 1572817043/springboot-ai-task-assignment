package com.example.taskai.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.taskai.user.entity.SysRole;
import com.example.taskai.user.entity.SysUser;
import com.example.taskai.user.mapper.SysRoleMapper;
import com.example.taskai.user.mapper.SysUserMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SysUserMapper sysUserMapper;

	@MockitoBean
	private SysRoleMapper sysRoleMapper;

	@Test
	void loginReturnsTokenAndUserInfo() throws Exception {
		SysUser user = new SysUser();
		user.setId(2L);
		user.setUsername("manager");
		user.setPasswordHash("manager123");
		user.setRealName("项目经理");
		user.setStatus("ENABLED");

		SysRole role = new SysRole();
		role.setRoleCode("MANAGER");

		when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
		when(sysRoleMapper.selectRoleByUserId(2L)).thenReturn(role);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"username":"manager","password":"manager123"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data.token").isNotEmpty())
			.andExpect(jsonPath("$.data.user.username").value("manager"))
			.andExpect(jsonPath("$.data.user.realName").value("项目经理"))
			.andExpect(jsonPath("$.data.user.role").value("MANAGER"));
	}

	@Test
	void loginRejectsInvalidPassword() throws Exception {
		SysUser user = new SysUser();
		user.setId(2L);
		user.setUsername("manager");
		user.setPasswordHash("manager123");
		user.setStatus("ENABLED");

		when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"username":"manager","password":"wrong-password"}
					"""))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value("账号或密码错误"));
	}

	@Test
	void loginRejectsDisabledUser() throws Exception {
		SysUser user = new SysUser();
		user.setId(2L);
		user.setUsername("manager");
		user.setPasswordHash("manager123");
		user.setStatus("DISABLED");

		when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"username":"manager","password":"manager123"}
					"""))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value(403))
			.andExpect(jsonPath("$.message").value("账号已被禁用"));
	}

	@Test
	void meReturnsCurrentUserFromBearerToken() throws Exception {
		SysUser user = new SysUser();
		user.setId(2L);
		user.setUsername("manager");
		user.setPasswordHash("manager123");
		user.setRealName("项目经理");
		user.setStatus("ENABLED");

		SysRole role = new SysRole();
		role.setRoleCode("MANAGER");

		when(sysUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
		when(sysRoleMapper.selectRoleByUserId(2L)).thenReturn(role);

		String token = loginAndReadToken();

		mockMvc.perform(get("/api/auth/me")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.data.username").value("manager"))
			.andExpect(jsonPath("$.data.role").value("MANAGER"));
	}

	@Test
	void meRejectsMissingToken() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value(401))
			.andExpect(jsonPath("$.message").value("未登录或登录已过期"));
	}

	private String loginAndReadToken() throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"username":"manager","password":"manager123"}
					"""))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return JsonPath.read(body, "$.data.token");
	}
}
