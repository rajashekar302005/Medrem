package com.example.medrem.repository;

import com.example.medrem.entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
	List<Medicine> findAllByUserUsername(String username);

	Optional<Medicine> findByIdAndUserUsername(Long id, String username);

	boolean existsByIdAndUserUsername(Long id, String username);
}
