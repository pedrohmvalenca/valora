package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.EntityType;
import java.time.Instant;
import java.util.UUID;

/** Linha de log retornada por GET /api/v1/logs (Admin only). Story 1.8. */
public record AuditLogDto(
        UUID id,
        UUID userId,
        String action,
        EntityType entityType,
        UUID entityId,
        UUID courseId,
        Instant createdAt
) {}
