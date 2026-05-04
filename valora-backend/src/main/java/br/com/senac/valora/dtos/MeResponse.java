package br.com.senac.valora.dtos;

/**
 * Body da resposta {@code 200 OK} de {@code GET /api/v1/auth/me}.
 *
 * <p>Tipo separado de {@link LoginResponse} por semântica — apesar da estrutura
 * idêntica, o significado é diferente: aqui é "quem sou eu agora?", lá é "fui
 * autenticado com sucesso".
 */
public record MeResponse(UserDto user) {}
