package com.example.taskai.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskResultSubmitRequest(
    @NotBlank(message = "成果摘要不能为空") String resultSummary,
    String resultUrl
) {
}
