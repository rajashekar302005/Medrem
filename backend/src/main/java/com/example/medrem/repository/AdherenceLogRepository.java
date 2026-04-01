package com.example.medrem.repository;

import com.example.medrem.entity.AdherenceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdherenceLogRepository extends JpaRepository<AdherenceLog, Long> {
    List<AdherenceLog> findAllByOrderByScheduledAtDesc();

    List<AdherenceLog> findAllByMedicineUserUsername(String username);

    List<AdherenceLog> findAllByMedicineUserUsernameOrderByScheduledAtDesc(String username);
}
