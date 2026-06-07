package br.com.senac.valora.dtos;

import java.util.List;
import java.util.UUID;

public record DashboardCourseDto(
        UUID courseId,
        String courseName,
        int minimumWorkloadHours,
        int totalRecognizedHours,
        int progressPercent,
        int pendingCount,
        List<DashboardCategoryDto> categories
) {}
