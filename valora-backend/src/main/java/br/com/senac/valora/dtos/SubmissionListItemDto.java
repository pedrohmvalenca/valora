package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

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
