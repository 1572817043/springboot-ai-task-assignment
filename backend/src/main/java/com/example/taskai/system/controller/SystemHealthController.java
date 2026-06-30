package com.example.taskai.system.controller;

import com.example.taskai.common.result.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemHealthController {

	@GetMapping("/health")
	public ApiResponse<HealthStatus> health() {
		return ApiResponse.ok(new HealthStatus("UP", "task-ai-backend"));
	}

	public record HealthStatus(String status, String service) {
	}
}
