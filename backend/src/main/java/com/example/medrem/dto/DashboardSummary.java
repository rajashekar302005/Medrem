package com.example.medrem.dto;

import java.util.List;

public record DashboardSummary(
        long totalDoses,
        long takenOnTime,
        long takenLate,
        long missed,
        double adherenceRate,
        double onTimeRate,
        List<MedicineAdherenceSummary> medicineBreakdown
) {
}
