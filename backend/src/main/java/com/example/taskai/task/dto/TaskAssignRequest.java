package com.example.taskai.task.dto;

import jakarta.validation.constraints.NotNull;

public record TaskAssignRequest(
    @NotNull(message = "assigneeId 不能为空") Long assigneeId,
    String reason
) {
}
