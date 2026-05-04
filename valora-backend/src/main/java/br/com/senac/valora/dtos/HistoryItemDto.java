package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Item do histórico do aluno na mesma categoria/curso (mockup — Story 4.5
 * nível C / Tier A γ). Linha compacta para o painel "Histórico do aluno".
 */
public record HistoryItemDto(
        UUID id,
        String description,
        int requestedHours,
        Integer recognizedHours,
        SubmissionStatus status,
        Instant createdAt,
        Instant decidedAt
) {}
