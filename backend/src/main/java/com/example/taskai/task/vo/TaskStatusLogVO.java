package com.example.taskai.task.vo;

import java.time.LocalDateTime;

public record TaskStatusLogVO(
    String oldStatus,
    String newStatus,
    String operatorName,
    String remark,
    LocalDateTime createdAt
) {
}
