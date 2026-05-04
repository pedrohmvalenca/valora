package br.com.senac.valora.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSubmissionRequest(
        @NotBlank(message = "Motivo é obrigatório")
        @Size(min = 20, max = 1000, message = "Motivo deve ter ao menos 20 caracteres (RN-0006)")
        String reason
) {}
