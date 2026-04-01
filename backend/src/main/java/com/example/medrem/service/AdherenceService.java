package com.example.medrem.service;

import com.example.medrem.dto.*;
import com.example.medrem.entity.AdherenceLog;
import com.example.medrem.entity.LogStatus;
import com.example.medrem.entity.Medicine;
import com.example.medrem.repository.AdherenceLogRepository;
import com.example.medrem.repository.MedicineRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdherenceService {
    private final AdherenceLogRepository adherenceLogRepository;
    private final MedicineRepository medicineRepository;

    public AdherenceService(AdherenceLogRepository adherenceLogRepository, MedicineRepository medicineRepository) {
        this.adherenceLogRepository = adherenceLogRepository;
        this.medicineRepository = medicineRepository;
    }

    public AdherenceLogResponse markTaken(LogActionRequest request, String username) {
        Medicine medicine = getMedicine(request.medicineId(), username);
        LocalDateTime actionAt = request.actionAt() != null ? request.actionAt() : LocalDateTime.now();

        LocalDateTime scheduledAt = request.scheduledAt() != null
                ? request.scheduledAt()
                : LocalDateTime.of(LocalDate.from(actionAt), medicine.getReminderTime());

        long minutesDiff = Math.abs(Duration.between(scheduledAt, actionAt).toMinutes());
        LogStatus status = minutesDiff <= 30 ? LogStatus.ON_TIME : LogStatus.LATE;

        AdherenceLog log = new AdherenceLog();
        log.setMedicine(medicine);
        log.setScheduledAt(scheduledAt);
        log.setActionAt(actionAt);
        log.setStatus(status);

        return toResponse(adherenceLogRepository.save(log));
    }

    public AdherenceLogResponse markMissed(LogActionRequest request, String username) {
        Medicine medicine = getMedicine(request.medicineId(), username);

        LocalDateTime scheduledAt = request.scheduledAt() != null
                ? request.scheduledAt()
                : LocalDateTime.of(LocalDate.now(), medicine.getReminderTime());

        AdherenceLog log = new AdherenceLog();
        log.setMedicine(medicine);
        log.setScheduledAt(scheduledAt);
        log.setActionAt(null);
        log.setStatus(LogStatus.MISSED);

        return toResponse(adherenceLogRepository.save(log));
    }

    public List<AdherenceLogResponse> listLogs(String username) {
        return adherenceLogRepository.findAllByMedicineUserUsernameOrderByScheduledAtDesc(username)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DashboardSummary getDashboardSummary(String username) {
        List<AdherenceLog> logs = adherenceLogRepository.findAllByMedicineUserUsername(username);

        long total = logs.size();
        long onTime = logs.stream().filter(log -> log.getStatus() == LogStatus.ON_TIME).count();
        long late = logs.stream().filter(log -> log.getStatus() == LogStatus.LATE).count();
        long missed = logs.stream().filter(log -> log.getStatus() == LogStatus.MISSED).count();

        double adherenceRate = total == 0 ? 0.0 : ((double) (onTime + late) / total) * 100.0;
        double onTimeRate = total == 0 ? 0.0 : ((double) onTime / total) * 100.0;

        Map<Long, List<AdherenceLog>> grouped = new HashMap<>();
        for (AdherenceLog log : logs) {
            Long medicineId = log.getMedicine().getId();
            grouped.computeIfAbsent(medicineId, key -> new ArrayList<>()).add(log);
        }

        List<MedicineAdherenceSummary> breakdown = grouped.values().stream().map(this::toMedicineSummary).toList();

        return new DashboardSummary(total, onTime, late, missed, adherenceRate, onTimeRate, breakdown);
    }

    private Medicine getMedicine(Long medicineId, String username) {
        return medicineRepository.findByIdAndUserUsername(medicineId, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));
    }

    private MedicineAdherenceSummary toMedicineSummary(List<AdherenceLog> logs) {
        if (logs.isEmpty()) {
            return new MedicineAdherenceSummary(null, "Unknown", 0, 0, 0, 0, 0.0);
        }

        Medicine medicine = logs.get(0).getMedicine();
        long total = logs.size();
        long onTime = logs.stream().filter(log -> log.getStatus() == LogStatus.ON_TIME).count();
        long late = logs.stream().filter(log -> log.getStatus() == LogStatus.LATE).count();
        long missed = logs.stream().filter(log -> log.getStatus() == LogStatus.MISSED).count();
        double adherenceRate = total == 0 ? 0.0 : ((double) (onTime + late) / total) * 100.0;

        return new MedicineAdherenceSummary(
                medicine.getId(),
                medicine.getName(),
                total,
                onTime,
                late,
                missed,
                adherenceRate
        );
    }

    private AdherenceLogResponse toResponse(AdherenceLog log) {
        return new AdherenceLogResponse(
                log.getId(),
                log.getMedicine().getId(),
                log.getMedicine().getName(),
                log.getScheduledAt(),
                log.getActionAt(),
                log.getStatus()
        );
    }
}
