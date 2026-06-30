package com.example.taskai.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record ProjectCreateRequest(
    @NotBlank(message = "项目名称不能为空") String projectName,
    String description,
    Long managerId,
    String status,
    LocalDate startDate,
    LocalDate endDate
) {
}
