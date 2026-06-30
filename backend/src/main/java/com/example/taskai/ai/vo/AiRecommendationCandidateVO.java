package com.example.taskai.ai.vo;

import java.math.BigDecimal;

public record AiRecommendationCandidateVO(
    Long candidateId,
    Long candidateUserId,
    String candidateName,
    Integer rankNo,
    BigDecimal totalScore,
    BigDecimal skillScore,
    BigDecimal historyScore,
    BigDecimal workloadScore,
    BigDecimal completionScore,
    BigDecimal deadlineRiskScore,
    String reason,
    Integer accepted
) {
}
