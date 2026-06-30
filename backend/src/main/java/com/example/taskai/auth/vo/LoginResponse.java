package com.example.taskai.auth.vo;

public record LoginResponse(
	String token,
	CurrentUserVO user
) {
}
