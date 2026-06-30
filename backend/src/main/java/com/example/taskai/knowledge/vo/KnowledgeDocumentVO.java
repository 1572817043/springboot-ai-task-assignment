package com.example.taskai.knowledge.vo;

import java.time.LocalDateTime;

public record KnowledgeDocumentVO(
    Long id,
    Long userId,
    String sourceType,
    Long sourceId,
    String title,
    String content,
    Integer indexed,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
