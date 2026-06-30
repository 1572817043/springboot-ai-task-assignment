package com.example.taskai.project.vo;

import java.time.LocalDateTime;

public record ProjectMemberVO(
    Long userId,
    String username,
    String realName,
    String projectRole,
    LocalDateTime joinedAt
) {
}
