package br.com.senac.valora.dtos;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record LinkStudentCoursesRequest(
        @NotEmpty(message = "Pelo menos 1 curso para vincular") List<UUID> courseIds
) {}
