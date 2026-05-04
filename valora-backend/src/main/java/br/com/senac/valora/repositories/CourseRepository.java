package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.Course;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, UUID> {
}
