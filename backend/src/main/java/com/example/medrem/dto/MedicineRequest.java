package com.example.medrem.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record MedicineRequest(
        @NotBlank String name,
        @NotBlank String dosageInstruction,
        @NotNull @Min(1) @Max(6) Integer frequencyPerDay,
        @NotNull LocalTime reminderTime,
        @NotNull LocalDate startDate,
        LocalDate endDate
) {
}
