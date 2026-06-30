package com.example.taskai.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserCreateRequest(
    @NotBlank(message = "用户名不能为空") String username,
    @NotBlank(message = "密码不能为空") String password,
    @NotBlank(message = "姓名不能为空") String realName,
    String email,
    String phone,
    @NotNull(message = "角色不能为空") Long roleId,
    String status
) {
}
