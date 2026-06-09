package br.com.senac.valora.dtos;

import java.util.UUID;

public record StudentSearchResultDto(
        UUID id,
        String registrationCode,
        String name,
        String email,
        boolean isActive,
        int linkedCourseCount
) {}
