package com.example.taskai.skill.dto;

public record SkillUpdateRequest(
    String skillName,
    String category,
    String description
) {
}
