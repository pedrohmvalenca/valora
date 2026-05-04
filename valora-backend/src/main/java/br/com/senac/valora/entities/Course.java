package br.com.senac.valora.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Curso (RF-0006). Mapeia tabela {@code courses} criada em V1.
 *
 * <p>Story 4.5 (consolidada γ) materializa esta entity (até então, só
 * {@link CoordinatorCourse} referenciava {@code courses} via FK; agora
 * Submissões + Categories também precisam consultar Course completo).
 *
 * <p>Vínculos N:N são mantidos em junctions separadas (CoordinatorCourse,
 * StudentCourse, CategoryCourse) — não modelados aqui via @OneToMany para
 * evitar lazy loading + N+1.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(name = "minimum_workload_hours", nullable = false)
    private int minimumWorkloadHours;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
