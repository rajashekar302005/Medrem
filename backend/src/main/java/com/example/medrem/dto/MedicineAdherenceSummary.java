package com.example.medrem.dto;

public record MedicineAdherenceSummary(
        Long medicineId,
        String medicineName,
        long total,
        long onTime,
        long late,
        long missed,
        double adherenceRate
) {
}
