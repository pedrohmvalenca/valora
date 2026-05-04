package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.Student;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, UUID> {

    /** Tier D γ — alunos vinculados a qualquer um dos cursos do escopo (Coord). */
    @Query(value = "SELECT DISTINCT s.* FROM students s "
            + "JOIN student_course sc ON sc.student_id = s.id "
            + "WHERE sc.course_id IN (:courseIds) "
            + "ORDER BY s.name ASC",
            nativeQuery = true)
    List<Student> findAllInCourses(@Param("courseIds") List<UUID> courseIds);
}
