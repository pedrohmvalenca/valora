package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.Category;
import java.util.UUID;

public record DashboardCategoryDto(
        UUID categoryId,
        String name,
        Category.GroupType groupType,
        BalanceDto balance
) {}
