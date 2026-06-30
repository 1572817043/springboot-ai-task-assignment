package com.example.taskai.user.vo;

import java.time.LocalDateTime;

public record UserListItemVO(
    Long id,
    String username,
    String realName,
    String email,
    String phone,
    String status,
    String roleCode,
    String roleName,
    LocalDateTime createdAt
) {
}
