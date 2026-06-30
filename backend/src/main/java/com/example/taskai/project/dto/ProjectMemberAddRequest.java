package com.example.taskai.project.dto;

import jakarta.validation.constraints.NotNull;

public record ProjectMemberAddRequest(
    @NotNull(message = "userId 不能为空") Long userId,
    String projectRole
) {
}
