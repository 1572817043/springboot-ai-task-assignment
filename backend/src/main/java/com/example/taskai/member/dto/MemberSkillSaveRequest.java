package com.example.taskai.member.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record MemberSkillSaveRequest(
    @NotEmpty(message = "技能列表不能为空") List<@Valid MemberSkillItemRequest> skills
) {
}
