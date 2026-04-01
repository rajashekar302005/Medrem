package com.example.medrem.service;

import com.example.medrem.dto.MedicineRequest;
import com.example.medrem.dto.MedicineResponse;
import com.example.medrem.entity.Medicine;
import com.example.medrem.entity.User;
import com.example.medrem.repository.MedicineRepository;
import com.example.medrem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MedicineService {
    private final MedicineRepository medicineRepository;
    private final UserRepository userRepository;

    public MedicineService(MedicineRepository medicineRepository, UserRepository userRepository) {
        this.medicineRepository = medicineRepository;
        this.userRepository = userRepository;
    }

    public MedicineResponse create(MedicineRequest request, String username) {
        validateDates(request);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Medicine medicine = new Medicine();
        medicine.setUser(user);
        medicine.setName(request.name());
        medicine.setDosageInstruction(request.dosageInstruction());
        medicine.setFrequencyPerDay(request.frequencyPerDay());
        medicine.setReminderTime(request.reminderTime());
        medicine.setStartDate(request.startDate());
        medicine.setEndDate(request.endDate());

        return toResponse(medicineRepository.save(medicine));
    }

    public List<MedicineResponse> listAll(String username) {
        return medicineRepository.findAllByUserUsername(username).stream().map(this::toResponse).toList();
    }

    public MedicineResponse update(Long id, MedicineRequest request, String username) {
        validateDates(request);

        Medicine medicine = medicineRepository.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found"));

        medicine.setName(request.name());
        medicine.setDosageInstruction(request.dosageInstruction());
        medicine.setFrequencyPerDay(request.frequencyPerDay());
        medicine.setReminderTime(request.reminderTime());
        medicine.setStartDate(request.startDate());
        medicine.setEndDate(request.endDate());

        return toResponse(medicineRepository.save(medicine));
    }

    public void delete(Long id, String username) {
        if (!medicineRepository.existsByIdAndUserUsername(id, username)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medicine not found");
        }
        medicineRepository.deleteById(id);
    }

    private void validateDates(MedicineRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date cannot be before start date");
        }
    }

    private MedicineResponse toResponse(Medicine medicine) {
        return new MedicineResponse(
                medicine.getId(),
                medicine.getName(),
                medicine.getDosageInstruction(),
                medicine.getFrequencyPerDay(),
                medicine.getReminderTime(),
                medicine.getStartDate(),
                medicine.getEndDate(),
                medicine.getCreatedAt()
        );
    }
}
