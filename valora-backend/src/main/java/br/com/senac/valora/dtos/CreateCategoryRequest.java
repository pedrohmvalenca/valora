package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.Category;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateCategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull Category.GroupType groupType,
        String description,
        @NotEmpty(message = "Pelo menos 1 curso com limite definido")
        @Valid List<CategoryCourseLimitDto> courseLimits
) {}
