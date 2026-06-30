package com.example.taskai.member.vo;

import java.math.BigDecimal;
import java.util.List;

public record MemberProfileDetailVO(
    Long userId,
    String username,
    String realName,
    String email,
    String phone,
    String resumeText,
    String experienceSummary,
    Integer currentWorkload,
    Integer completedTaskCount,
    Integer overdueTaskCount,
    BigDecimal taskCompletionRate,
    BigDecimal overdueRate,
    List<MemberSkillVO> skills
) {
}
