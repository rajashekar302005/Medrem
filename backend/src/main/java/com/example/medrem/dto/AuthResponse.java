package com.example.medrem.dto;

public record AuthResponse(
        String token,
        String username
) {
}
