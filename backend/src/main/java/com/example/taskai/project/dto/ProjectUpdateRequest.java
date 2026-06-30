package com.example.taskai.project.dto;

import java.time.LocalDate;

public record ProjectUpdateRequest(
    String projectName,
    String description,
    Long managerId,
    String status,
    LocalDate startDate,
    LocalDate endDate
) {
}
