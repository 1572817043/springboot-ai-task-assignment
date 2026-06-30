package com.example.taskai.task.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TaskRequiredSkillRequest(
    @NotNull(message = "skillId 不能为空") Long skillId,
    BigDecimal weight
) {
}
