package br.com.senac.valora.dtos;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body de {@code POST /api/v1/auth/change-password} (Story 1.11).
 *
 * <p>{@code newPassword} mínimo 8 chars — política da Story 1.11 (Dev Notes:
 * "sem complexidade obrigatória nesta versão"). {@code currentPassword} sem
 * tamanho mínimo na anotação — qualquer senha persistida é aceita como input;
 * a validação real acontece no BCrypt {@code matches()} (401 quando errada).
 * {@code max=100} em ambos os campos defende contra payload arbitrariamente
 * grande passar pelo Jackson antes de chegar no BCrypt (patch code review 2026-06-09).
 *
 * <p>{@link #isNewPasswordDifferent()} usa {@code @AssertTrue} para que Bean Validation
 * rejeite payload com {@code newPassword == currentPassword} já na borda. Sem isso,
 * cliente API-savvy podia chamar o endpoint com a provisória nos dois campos, zerar
 * a flag {@code must_change_password} e continuar usando a senha provisória —
 * defeats da intenção de segurança da Story 1.11 (patch code review 2026-06-09).
 */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 100) String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword) {

    @AssertTrue(message = "A nova senha não pode ser igual à atual")
    public boolean isNewPasswordDifferent() {
        // Tolerante a null/blank — @NotBlank já cobre os campos individualmente; aqui
        // só rejeita o caso em que ambos têm valor e são iguais. Caso contrário, deixa
        // os erros isolados de @NotBlank/@Size virem primeiro.
        return currentPassword == null || newPassword == null || !newPassword.equals(currentPassword);
    }
}
