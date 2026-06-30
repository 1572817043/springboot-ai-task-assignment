package com.example.taskai.task.vo;

import java.time.LocalDateTime;

public record TaskResultVO(
    String resultSummary,
    String resultUrl,
    String reviewStatus,
    String reviewComment,
    LocalDateTime submittedAt,
    LocalDateTime reviewedAt
) {
}
