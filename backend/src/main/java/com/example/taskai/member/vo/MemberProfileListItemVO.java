package com.example.taskai.member.vo;

import java.math.BigDecimal;
import java.util.List;

public record MemberProfileListItemVO(
    Long userId,
    String username,
    String realName,
    String email,
    Integer currentWorkload,
    Integer completedTaskCount,
    Integer overdueTaskCount,
    BigDecimal taskCompletionRate,
    BigDecimal overdueRate,
    List<String> skills
) {
}
