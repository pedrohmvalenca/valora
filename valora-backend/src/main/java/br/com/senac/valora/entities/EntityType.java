package br.com.senac.valora.entities;

/**
 * Discriminador para {@link AuditLog#entityType} (RF-0033 + EXT-06).
 *
 * <p>CHECK constraint em {@code audit_log.entity_type} (V1) já valida estes
 * valores; manter sincronizado se adicionar novo.
 */
public enum EntityType {
    USER, COURSE, COORDINATOR, STUDENT, CATEGORY, SUBMISSION
}
