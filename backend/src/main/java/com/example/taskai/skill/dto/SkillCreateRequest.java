package com.example.taskai.skill.dto;

import jakarta.validation.constraints.NotBlank;

public record SkillCreateRequest(
    @NotBlank(message = "技能名称不能为空") String skillName,
    String category,
    String description
) {
}
