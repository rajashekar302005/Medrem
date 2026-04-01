package com.example.medrem.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record LogActionRequest(
        @NotNull Long medicineId,
        LocalDateTime actionAt,
        LocalDateTime scheduledAt
) {
}
