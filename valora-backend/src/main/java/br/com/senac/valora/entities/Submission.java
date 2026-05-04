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
 * Submissão de Atividade Complementar (RF-0023..RF-0028 + RN-0004 + RN-0006 +
 * RN-0009). Story 4.5 (consolidada γ) — schema V3.
 *
 * <p>Relacionamentos modelados como UUIDs simples (não @ManyToOne) para evitar
 * lazy-loading + N+1 na lista; nomes de student/course/category são resolvidos
 * via JOIN ou queries dedicadas no service.
 */
@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private String description;

    @Column(name = "requested_hours", nullable = false)
    private int requestedHours;

    /**
     * Horas efetivamente reconhecidas na aprovação (aprovação parcial — RN-0004).
     * NULL enquanto status=PENDING/REJECTED; obrigatório quando APPROVED.
     * Story 4.5 nível C (Tier A γ).
     */
    @Column(name = "recognized_hours")
    private Integer recognizedHours;

    @Column(name = "proof_path")
    private String proofPath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
