package br.com.senac.valora.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trilha de auditoria — RF-0032 + RF-0033 + EXT-06.
 *
 * <p>Mapeia tabela {@code audit_log} criada na V1. Story 1.8 implementa
 * {@code AuditAspect} (AOP) que persiste registros quando métodos anotados
 * com {@code @Auditable} são executados com sucesso.
 *
 * <p>{@code payloadJson} fica null nesta story; futuras versões podem armazenar
 * delta antes/depois (com filtro de campos sensíveis).
 */
@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private EntityType entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Column(name = "course_id")
    private UUID courseId;

    // payload_json (JSONB no banco) NÃO é mapeado nesta story — sempre NULL.
    // Story 1.8.x futura adicionará com @JdbcTypeCode(SqlTypes.JSON) ou
    // conversor explícito quando delta antes/depois for necessário.

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
