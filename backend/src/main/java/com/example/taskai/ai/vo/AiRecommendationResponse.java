package com.example.taskai.ai.vo;

import java.util.List;

public record AiRecommendationResponse(
    Long batchId,
    Long taskId,
    List<AiRecommendationCandidateVO> candidates
) {
}
