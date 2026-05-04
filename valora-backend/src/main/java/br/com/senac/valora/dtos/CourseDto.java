package br.com.senac.valora.dtos;

import java.time.Instant;
import java.util.UUID;

public record CourseDto(
        UUID id,
        String name,
        String code,
        int minimumWorkloadHours,
        boolean isActive,
        Instant createdAt
) {}
