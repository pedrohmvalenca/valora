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
