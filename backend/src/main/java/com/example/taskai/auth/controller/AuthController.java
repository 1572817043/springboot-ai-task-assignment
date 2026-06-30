package com.example.taskai.auth.controller;

import com.example.taskai.auth.dto.LoginRequest;
import com.example.taskai.auth.service.AuthService;
import com.example.taskai.auth.vo.CurrentUserVO;
import com.example.taskai.auth.vo.LoginResponse;
import com.example.taskai.common.result.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		return ApiResponse.ok(authService.login(request));
	}

	@GetMapping("/me")
	public ApiResponse<CurrentUserVO> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
		return ApiResponse.ok(authService.currentUser(authorization));
	}
}
