package com.example.medrem.dto;

import com.example.medrem.entity.LogStatus;

import java.time.LocalDateTime;

public record AdherenceLogResponse(
        Long id,
        Long medicineId,
        String medicineName,
        LocalDateTime scheduledAt,
        LocalDateTime actionAt,
        LogStatus status
) {
}
