package br.com.senac.valora.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record CategoryCourseLimitDto(
        @NotNull UUID courseId,
        @NotNull @Positive Integer maxHours
) {}
