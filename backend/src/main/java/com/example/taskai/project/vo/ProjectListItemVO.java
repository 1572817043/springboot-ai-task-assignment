package com.example.taskai.project.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectListItemVO(
    Long id,
    String projectName,
    String description,
    Long managerId,
    String managerName,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Integer memberCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
