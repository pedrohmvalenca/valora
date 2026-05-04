package br.com.senac.valora.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * PK composta da junction {@link CoordinatorCourse} (Coordinator N:N Course).
 *
 * <p>Mapeia colunas {@code coordinator_id} + {@code course_id} da tabela
 * {@code coordinator_course}. Implementa {@link Serializable} e
 * {@code equals/hashCode} (via Lombok) por contrato JPA para chaves embutidas.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class CoordinatorCourseId implements Serializable {

    @Column(name = "coordinator_id", nullable = false)
    private UUID coordinatorId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;
}
