package br.com.senac.valora.dtos;

import java.time.Instant;
import java.util.UUID;

/**
 * Vínculo enriquecido aluno × curso — Story 3.10.
 *
 * <p>Retornado por {@code GET /students/{id}/courses} (lista) e
 * {@code PATCH /students/{id}/courses/{courseId}/status} (após mudança).
 * Inclui o {@code status} do vínculo (CURSANDO / CONCLUIDO / ABANDONADO)
 * e o code/name do curso para evitar round-trip extra no frontend.
 */
public record StudentCourseStatusDto(
        UUID courseId,
        String courseCode,
        String courseName,
        String status,
        Instant createdAt
) {}
