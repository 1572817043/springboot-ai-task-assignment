package com.example.taskai.system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemHealthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsHealthStatus() throws Exception {
		mockMvc.perform(get("/api/system/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.code").value(200))
			.andExpect(jsonPath("$.message").value("ok"))
			.andExpect(jsonPath("$.data.status").value("UP"))
			.andExpect(jsonPath("$.data.service").value("task-ai-backend"));
	}

	@Test
	void returnsUnifiedResponseWhenMethodIsNotAllowed() throws Exception {
		mockMvc.perform(post("/api/system/health"))
			.andExpect(status().isMethodNotAllowed())
			.andExpect(jsonPath("$.code").value(405))
			.andExpect(jsonPath("$.message").value("请求方法不支持"));
	}
}
