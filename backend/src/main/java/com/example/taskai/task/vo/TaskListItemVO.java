package com.example.taskai.task.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TaskListItemVO(
    Long id,
    Long projectId,
    String projectName,
    String title,
    String priority,
    String status,
    Long creatorId,
    String creatorName,
    Long assigneeId,
    String assigneeName,
    LocalDateTime deadline,
    BigDecimal estimatedHours,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
