package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

public record HistoryItemDto(
        UUID id,
        String description,
        int requestedHours,
        Integer recognizedHours,
        SubmissionStatus status,
        Instant createdAt,
        Instant decidedAt
) {}
