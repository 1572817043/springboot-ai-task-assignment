package com.example.taskai.user.dto;

public record UserUpdateRequest(
    String realName,
    String email,
    String phone,
    Long roleId,
    String status
) {
}
