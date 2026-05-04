package br.com.senac.valora.dtos;

import br.com.senac.valora.entities.UserProfile;
import java.util.List;
import java.util.UUID;

/**
 * Payload do usuário autenticado retornado em {@code /auth/login} e {@code /auth/me}.
 *
 * <p>{@code linkedCourses} carrega apenas UUIDs nesta story — entidade
 * {@code Course} completa só nasce na Story 2.1. Para Admin e Student na
 * Entrega 1, o array é sempre vazio (Admin opera em todos os cursos via
 * {@code <CursoSelector>}; Student não tem vínculos persistidos em V1).
 */
public record UserDto(
        UUID id,
        String name,
        UserProfile profile,
        List<UUID> linkedCourses) {}
