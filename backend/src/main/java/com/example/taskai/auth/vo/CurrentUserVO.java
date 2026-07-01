package com.example.taskai.auth.vo;

public record CurrentUserVO(
	Long id,
	String username,
	String realName,
	String role
) {
}
