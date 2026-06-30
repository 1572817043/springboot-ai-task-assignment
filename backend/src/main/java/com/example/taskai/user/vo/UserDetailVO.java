package com.example.taskai.user.vo;

import java.time.LocalDateTime;

public record UserDetailVO(
    Long id,
    String username,
    String realName,
    String email,
    String phone,
    String avatarUrl,
    String status,
    Long roleId,
    String roleCode,
    String roleName,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
