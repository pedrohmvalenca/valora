package br.com.senac.valora.dtos;

import jakarta.validation.constraints.Positive;

public record ApproveSubmissionRequest(
        @Positive(message = "Horas reconhecidas devem ser positivas")
        Integer recognizedHours
) {}
