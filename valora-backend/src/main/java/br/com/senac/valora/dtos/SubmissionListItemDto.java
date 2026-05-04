package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

/** Linha da listagem de submissões. Story 4.5 (consolidada γ). */
public record SubmissionListItemDto(
        UUID id,
        String studentName,
        String studentRegistration,
        String courseName,
        String categoryName,
        int requestedHours,
        SubmissionStatus status,
        Instant createdAt
) {}
