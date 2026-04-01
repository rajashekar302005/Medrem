package com.example.medrem.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record MedicineResponse(
        Long id,
        String name,
        String dosageInstruction,
        Integer frequencyPerDay,
        LocalTime reminderTime,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt
) {
}
