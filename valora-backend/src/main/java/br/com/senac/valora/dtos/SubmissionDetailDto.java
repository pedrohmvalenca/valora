package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionDetailDto(
        UUID id,
        UUID studentId,
        String studentName,
        String studentRegistration,
        UUID courseId,
        String courseName,
        UUID categoryId,
        String categoryName,
        Category.GroupType categoryGroup,
        String description,
        int requestedHours,
        Integer recognizedHours,
        String proofPath,
        SubmissionStatus status,
        UUID decidedBy,
        Instant decidedAt,
        String rejectionReason,
        Instant createdAt,
        BalanceDto balance,
        List<HistoryItemDto> history
) {}
