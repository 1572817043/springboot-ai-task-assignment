package com.example.taskai.auth.vo;

public record CurrentUserVO(
	String username,
	String realName,
	String role
) {
}
