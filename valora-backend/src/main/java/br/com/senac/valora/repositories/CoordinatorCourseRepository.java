package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.CoordinatorCourse;
import br.com.senac.valora.entities.CoordinatorCourseId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository da junction {@link CoordinatorCourse}.
 *
 * <p>O método {@link #findCourseIdsByCoordinatorId(UUID)} é otimizado para o
 * fluxo de login: retorna apenas os UUIDs dos cursos vinculados, sem carregar
 * o grafo de entidades. Mantém o JWT pequeno e a resposta da API leve.
 */
@Repository
public interface CoordinatorCourseRepository
        extends JpaRepository<CoordinatorCourse, CoordinatorCourseId> {

    /**
     * Busca os IDs dos cursos vinculados a um coordenador.
     *
     * @param coordinatorId UUID do usuário com profile=COORDINATOR
     * @return lista possivelmente vazia de course IDs (ordem não garantida)
     */
    @Query("SELECT cc.id.courseId FROM CoordinatorCourse cc WHERE cc.id.coordinatorId = :coordinatorId")
    List<UUID> findCourseIdsByCoordinatorId(@Param("coordinatorId") UUID coordinatorId);
}
