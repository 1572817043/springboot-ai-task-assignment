package com.example.taskai.task.dto;

import jakarta.validation.constraints.NotBlank;

public record TaskStatusRequest(
    @NotBlank(message = "状态不能为空") String status,
    String remark
) {
}
