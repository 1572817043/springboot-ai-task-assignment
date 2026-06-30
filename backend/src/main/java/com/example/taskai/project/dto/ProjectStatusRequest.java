package com.example.taskai.project.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectStatusRequest(
    @NotBlank(message = "状态不能为空") String status
) {
}
