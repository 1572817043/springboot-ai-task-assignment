package com.example.taskai.skill.vo;

import java.time.LocalDateTime;

public record SkillListItemVO(
    Long id,
    String skillName,
    String category,
    String description,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
