package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.CategoryCourse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryCourseRepository extends JpaRepository<CategoryCourse, CategoryCourse.PK> {

    /** Lookup do limite de horas por (categoria × curso) — usado no cálculo de saldo. */
    Optional<CategoryCourse> findByCategoryIdAndCourseId(UUID categoryId, UUID courseId);

    /** Tier E γ — listar todos os limites de uma categoria (1 por curso). */
    java.util.List<CategoryCourse> findByCategoryId(UUID categoryId);
}
