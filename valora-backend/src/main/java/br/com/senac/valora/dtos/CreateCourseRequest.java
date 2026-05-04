package br.com.senac.valora.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateCourseRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 20) String code,
        @Positive Integer minimumWorkloadHours
) {}
