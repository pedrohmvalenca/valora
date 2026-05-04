package br.com.senac.valora.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body do POST /submissions/{id}/reject. Motivo ≥ 20 chars (RN-0006).
 * Story 4.5 (consolidada γ).
 */
public record RejectSubmissionRequest(
        @NotBlank(message = "Motivo é obrigatório")
        @Size(min = 20, max = 1000, message = "Motivo deve ter ao menos 20 caracteres (RN-0006)")
        String reason
) {}
