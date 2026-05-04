package br.com.senac.valora.mappers;

import br.com.senac.valora.dtos.UserDto;
import br.com.senac.valora.entities.User;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper MapStruct {@link User} → {@link UserDto}.
 *
 * <p>{@code linkedCourses} é passado como segundo parâmetro porque não está
 * presente em {@code User} (junction {@code coordinator_course} é uma tabela
 * separada). MapStruct copia os demais campos por nome.
 *
 * <p>Adicionado em ADR-0013 (convergência com {@code cldavi/valoraapi} —
 * MapStruct para mapping DTO ↔ Entity em vez de mapeamento manual).
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "name", source = "user.name")
    @Mapping(target = "profile", source = "user.profile")
    @Mapping(target = "linkedCourses", source = "linkedCourses")
    UserDto toDto(User user, List<UUID> linkedCourses);
}
