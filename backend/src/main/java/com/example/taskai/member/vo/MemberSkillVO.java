package com.example.taskai.member.vo;

import java.math.BigDecimal;

public record MemberSkillVO(
    Long skillId,
    String skillName,
    String category,
    Integer level,
    BigDecimal years,
    String description
) {
}
