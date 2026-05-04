package br.com.senac.valora.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StudentDto(
        UUID id,
        String registrationCode,
        String name,
        String email,
        boolean isActive,
        List<UUID> linkedCourseIds,
        Instant createdAt
) {}
