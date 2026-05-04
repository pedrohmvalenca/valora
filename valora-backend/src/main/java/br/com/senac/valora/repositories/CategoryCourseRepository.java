package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.CategoryCourse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryCourseRepository extends JpaRepository<CategoryCourse, CategoryCourse.PK> {

    Optional<CategoryCourse> findByCategoryIdAndCourseId(UUID categoryId, UUID courseId);

    java.util.List<CategoryCourse> findByCategoryId(UUID categoryId);
}
