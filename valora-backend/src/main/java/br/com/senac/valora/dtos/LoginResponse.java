package br.com.senac.valora.dtos;

/**
 * Body da resposta {@code 200 OK} de {@code POST /api/v1/auth/login}.
 *
 * <p>O JWT segue no {@code Set-Cookie} httpOnly (consumido pelo PWA) e também
 * no campo {@code token} do body, lido pelo app mobile (Bearer dual-mode).
 * A web ignora o {@code token}; o mobile guarda no {@code expo-secure-store}.
 */
public record LoginResponse(UserDto user, String token) {}
