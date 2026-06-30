package com.example.taskai.project.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ProjectDetailVO(
    Long id,
    String projectName,
    String description,
    Long managerId,
    String managerName,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<ProjectMemberVO> members
) {
}
