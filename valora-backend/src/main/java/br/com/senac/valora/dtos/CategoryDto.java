package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.Category;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CategoryDto(
        UUID id,
        String name,
        Category.GroupType groupType,
        String description,
        List<CategoryCourseLimitDto> courseLimits,
        Instant createdAt
) {}
