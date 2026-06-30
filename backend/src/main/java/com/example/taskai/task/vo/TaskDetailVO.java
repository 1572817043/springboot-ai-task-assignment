package com.example.taskai.task.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailVO(
    Long id,
    Long projectId,
    String projectName,
    String title,
    String description,
    String priority,
    String status,
    Long creatorId,
    String creatorName,
    Long assigneeId,
    String assigneeName,
    LocalDateTime deadline,
    BigDecimal estimatedHours,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<TaskRequiredSkillVO> requiredSkills,
    List<TaskStatusLogVO> statusLogs,
    TaskResultVO latestResult
) {
}
