package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

public record MySubmissionDto(
        UUID id,
        UUID courseId,
        String courseName,
        UUID categoryId,
        String categoryName,
        Category.GroupType groupType,
        String description,
        int requestedHours,
        Integer recognizedHours,
        SubmissionStatus status,
        String rejectionReason,
        Instant createdAt
) {}
