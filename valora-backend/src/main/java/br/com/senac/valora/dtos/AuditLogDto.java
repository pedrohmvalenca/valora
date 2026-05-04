package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.EntityType;
import java.time.Instant;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID userId,
        String action,
        EntityType entityType,
        UUID entityId,
        UUID courseId,
        Instant createdAt
) {}
