package com.example.taskai.user.dto;

import jakarta.validation.constraints.NotBlank;

public record UserStatusRequest(
    @NotBlank(message = "状态不能为空") String status
) {
}
