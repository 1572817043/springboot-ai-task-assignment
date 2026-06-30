package com.example.taskai.task.dto;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TaskUpdateRequest(
    String title,
    String description,
    String priority,
    LocalDateTime deadline,
    BigDecimal estimatedHours,
    List<@Valid TaskRequiredSkillRequest> requiredSkills
) {
}
