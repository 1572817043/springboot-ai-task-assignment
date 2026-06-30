package com.example.taskai.member.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record MemberSkillItemRequest(
    @NotNull(message = "skillId 不能为空") Long skillId,
    @NotNull(message = "level 不能为空") @Min(value = 1, message = "level 最小为 1") @Max(value = 5, message = "level 最大为 5") Integer level,
    BigDecimal years,
    String description
) {
}
