package br.com.senac.valora.entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Junction table N:N entre Coordinator (User com profile=COORDINATOR) e Course.
 *
 * <p>Modelada como junction "puro" nesta story: <b>sem</b> {@code @ManyToOne}
 * para User ou Course. Motivos:
 * <ul>
 *   <li>{@code Course} entity só nasce na Story 2.1 — não pode existir referência ainda.</li>
 *   <li>Acesso aos vínculos no login é feito via query de IDs em
 *       {@link br.com.senac.valora.repositories.CoordinatorCourseRepository#findCourseIdsByCoordinatorId(java.util.UUID)},
 *       sem necessidade de carregar grafos completos.</li>
 * </ul>
 */
@Entity
@Table(name = "coordinator_course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoordinatorCourse {

    @EmbeddedId
    private CoordinatorCourseId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
