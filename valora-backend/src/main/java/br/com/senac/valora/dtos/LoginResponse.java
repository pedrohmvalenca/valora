package br.com.senac.valora.dtos;

/**
 * Body da resposta {@code 200 OK} de {@code POST /api/v1/auth/login}.
 *
 * <p>O JWT vai apenas no {@code Set-Cookie}; o body carrega só o usuário —
 * defesa contra XSS (token nunca cai no DOM).
 */
public record LoginResponse(UserDto user) {}
