package com.example.medrem.controller;

import com.example.medrem.dto.AdherenceLogResponse;
import com.example.medrem.dto.DashboardSummary;
import com.example.medrem.dto.LogActionRequest;
import com.example.medrem.service.AdherenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/adherence")
public class AdherenceController {
    private final AdherenceService adherenceService;

    public AdherenceController(AdherenceService adherenceService) {
        this.adherenceService = adherenceService;
    }

    @PostMapping("/take")
    public AdherenceLogResponse markTaken(@Valid @RequestBody LogActionRequest request, Principal principal) {
        return adherenceService.markTaken(request, principal.getName());
    }

    @PostMapping("/miss")
    public AdherenceLogResponse markMissed(@Valid @RequestBody LogActionRequest request, Principal principal) {
        return adherenceService.markMissed(request, principal.getName());
    }

    @GetMapping("/logs")
    public List<AdherenceLogResponse> listLogs(Principal principal) {
        return adherenceService.listLogs(principal.getName());
    }

    @GetMapping("/summary")
    public DashboardSummary getSummary(Principal principal) {
        return adherenceService.getDashboardSummary(principal.getName());
    }
}
