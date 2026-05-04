package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.CoordinatorCourse;
import br.com.senac.valora.entities.CoordinatorCourseId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CoordinatorCourseRepository
        extends JpaRepository<CoordinatorCourse, CoordinatorCourseId> {

    @Query("SELECT cc.id.courseId FROM CoordinatorCourse cc WHERE cc.id.coordinatorId = :coordinatorId")
    List<UUID> findCourseIdsByCoordinatorId(@Param("coordinatorId") UUID coordinatorId);
}
