package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.Submission;
import br.com.senac.valora.entities.SubmissionStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    /** Listagem paginada filtrada por escopo (cursos vinculados ao Coord) + status opcional. */
    Page<Submission> findByCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds, Pageable pageable);

    Page<Submission> findByCourseIdInAndStatusOrderByCreatedAtDesc(
            List<UUID> courseIds, SubmissionStatus status, Pageable pageable);

    /** Listagem global (Admin sem filtro de curso). */
    Page<Submission> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Submission> findByStatusOrderByCreatedAtDesc(SubmissionStatus status, Pageable pageable);

    Page<Submission> findByCourseIdOrderByCreatedAtDesc(UUID courseId, Pageable pageable);

    Page<Submission> findByCourseIdAndStatusOrderByCreatedAtDesc(
            UUID courseId, SubmissionStatus status, Pageable pageable);

    @Query("SELECT COALESCE(SUM(s.recognizedHours), 0) FROM Submission s "
            + "WHERE s.studentId = :studentId AND s.courseId = :courseId "
            + "AND s.categoryId = :categoryId AND s.status = 'APPROVED'")
    int sumApprovedHours(@Param("studentId") UUID studentId,
                         @Param("courseId") UUID courseId,
                         @Param("categoryId") UUID categoryId);

    java.util.List<Submission> findTop10ByStudentIdAndCourseIdAndCategoryIdAndIdNotOrderByCreatedAtDesc(
            UUID studentId, UUID courseId, UUID categoryId, UUID excludeId);
}
