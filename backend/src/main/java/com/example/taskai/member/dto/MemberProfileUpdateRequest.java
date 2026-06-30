package com.example.taskai.member.dto;

import java.math.BigDecimal;

public record MemberProfileUpdateRequest(
    String resumeText,
    String experienceSummary,
    Integer currentWorkload,
    Integer completedTaskCount,
    Integer overdueTaskCount,
    BigDecimal taskCompletionRate,
    BigDecimal overdueRate
) {
}
