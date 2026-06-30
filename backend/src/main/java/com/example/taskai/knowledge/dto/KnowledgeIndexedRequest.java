package com.example.taskai.knowledge.dto;

import jakarta.validation.constraints.NotNull;

public record KnowledgeIndexedRequest(
    @NotNull(message = "indexed 不能为空") Integer indexed
) {
}
