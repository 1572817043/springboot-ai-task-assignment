package com.example.taskai.task.vo;

import java.math.BigDecimal;

public record TaskRequiredSkillVO(
    Long skillId,
    String skillName,
    String category,
    BigDecimal weight
) {
}
