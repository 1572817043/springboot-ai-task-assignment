package com.example.taskai.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskResultReviewRequest(
    @NotBlank(message = "reviewStatus 不能为空") String reviewStatus,
    String reviewComment
) {
}
