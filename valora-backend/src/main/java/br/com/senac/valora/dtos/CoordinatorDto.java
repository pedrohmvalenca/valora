package br.com.senac.valora.dtos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoordinatorDto(
        UUID id,
        String name,
        String email,
        boolean isActive,
        List<UUID> linkedCourseIds,
        Instant createdAt
) {}
