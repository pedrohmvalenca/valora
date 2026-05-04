package br.com.senac.valora.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /api/v1/auth/login}.
 *
 * <p>Validação Bean Validation roda no Controller via {@code @Valid} —
 * AuthService nunca recebe email vazio ou inválido.
 */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String password) {}
