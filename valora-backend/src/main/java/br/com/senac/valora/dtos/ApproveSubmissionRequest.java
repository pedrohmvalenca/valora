package br.com.senac.valora.dtos;

import jakarta.validation.constraints.Positive;

/**
 * Body opcional do POST /submissions/{id}/approve. Story 4.5 nível C (Tier A γ).
 *
 * <p>Se {@code recognizedHours} for null/ausente, aprova com {@code requestedHours}
 * da submissão (caminho normal). Se vier valor, aprova parcial — DEVE ser
 * positivo e ≤ requested_hours; o service também valida que ≤ saldo restante.
 */
public record ApproveSubmissionRequest(
        @Positive(message = "Horas reconhecidas devem ser positivas")
        Integer recognizedHours
) {}
