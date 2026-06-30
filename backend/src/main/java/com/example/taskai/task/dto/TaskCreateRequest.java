package com.example.taskai.task.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TaskCreateRequest(
    @NotNull(message = "projectId 不能为空") Long projectId,
    @NotBlank(message = "任务标题不能为空") String title,
    String description,
    String priority,
    LocalDateTime deadline,
    BigDecimal estimatedHours,
    List<@Valid TaskRequiredSkillRequest> requiredSkills
) {
}
