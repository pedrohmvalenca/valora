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
