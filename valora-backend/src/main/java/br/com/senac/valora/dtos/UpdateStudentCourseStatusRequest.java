package br.com.senac.valora.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Payload de {@code PATCH /students/{id}/courses/{courseId}/status} — Story 3.10.
 *
 * <p>{@code @Pattern} garante 400 (via {@code GlobalExceptionHandler.handleValidation})
 * quando um valor fora do enum chega — antes mesmo de o controller rodar. Mantém
 * paridade com o {@code CHECK} do SQL (V104).
 */
public record UpdateStudentCourseStatusRequest(
        @NotNull
        @Pattern(regexp = "CURSANDO|CONCLUIDO|ABANDONADO",
                message = "status deve ser CURSANDO, CONCLUIDO ou ABANDONADO")
        String status
) {}
