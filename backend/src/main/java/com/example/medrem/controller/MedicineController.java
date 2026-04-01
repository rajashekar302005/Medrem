package com.example.medrem.controller;

import com.example.medrem.dto.MedicineRequest;
import com.example.medrem.dto.MedicineResponse;
import com.example.medrem.service.MedicineService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/medicines")
public class MedicineController {
    private final MedicineService medicineService;

    public MedicineController(MedicineService medicineService) {
        this.medicineService = medicineService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicineResponse create(@Valid @RequestBody MedicineRequest request, Principal principal) {
        return medicineService.create(request, principal.getName());
    }

    @GetMapping
    public List<MedicineResponse> list(Principal principal) {
        return medicineService.listAll(principal.getName());
    }

    @PutMapping("/{id}")
    public MedicineResponse update(@PathVariable Long id, @Valid @RequestBody MedicineRequest request, Principal principal) {
        return medicineService.update(id, request, principal.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Principal principal) {
        medicineService.delete(id, principal.getName());
    }
}
