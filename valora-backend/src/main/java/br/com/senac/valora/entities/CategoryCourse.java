package br.com.senac.valora.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Limite de horas por (categoria × curso) — RF-0020.
 *
 * <p>Story 4.5 (consolidada γ). PK composta via {@link CategoryCourse.PK}.
 * Carrega o atributo {@code maxHours} usado no cálculo de saldo (RN-0004).
 */
@Entity
@Table(name = "category_course")
@IdClass(CategoryCourse.PK.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCourse {

    @Id
    @Column(name = "category_id")
    private UUID categoryId;

    @Id
    @Column(name = "course_id")
    private UUID courseId;

    @Column(name = "max_hours", nullable = false)
    private int maxHours;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Composite PK — necessária para {@code @IdClass}. */
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @Getter
    @Setter
    public static class PK implements Serializable {
        private UUID categoryId;
        private UUID courseId;
    }
}
