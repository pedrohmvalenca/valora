package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CreateStudentRequest;
import br.com.senac.valora.dtos.StudentDto;
import br.com.senac.valora.entities.EntityType;
import br.com.senac.valora.entities.Student;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.repositories.StudentRepository;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.AuditService;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Story Epic 3 (γ) — CRUD de alunos (Coord vê só vinculados, Admin vê tudo).
 *
 * Aluno NÃO loga nesta sessão — só dados administrativos. Story 5.1 (futura)
 * adiciona User profile=STUDENT pra aluno se autenticar.
 */
@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class StudentController {

    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;
    private final CoordinatorCourseRepository ccRepo;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager em;

    public StudentController(StudentRepository studentRepo, CourseRepository courseRepo,
                             CoordinatorCourseRepository ccRepo, AuditService auditService) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
        this.ccRepo = ccRepo;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<StudentDto>> list(JwtAuthentication auth) {
        List<Student> students;
        if (auth.profile() == UserProfile.ADMINISTRATOR) {
            students = studentRepo.findAll();
        } else {
            List<UUID> linked = ccRepo.findCourseIdsByCoordinatorId(auth.userId());
            students = linked.isEmpty() ? List.of() : studentRepo.findAllInCourses(linked);
        }
        List<StudentDto> result = students.stream().map(s -> {
            @SuppressWarnings("unchecked")
            List<UUID> courseIds = em.createNativeQuery(
                    "SELECT course_id FROM student_course WHERE student_id = :sid")
                    .setParameter("sid", s.getId())
                    .getResultList()
                    .stream()
                    .map(o -> (UUID) o)
                    .toList();
            return new StudentDto(s.getId(), s.getRegistrationCode(), s.getName(), s.getEmail(),
                    s.isActive(), courseIds, s.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<StudentDto> create(@Valid @RequestBody CreateStudentRequest req,
                                             JwtAuthentication auth) {
        // Validar que todos os cursos existem
        for (UUID courseId : req.courseIds()) {
            if (courseRepo.findById(courseId).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.NOT_FOUND,
                        "Curso não encontrado: id=" + courseId);
            }
            // Coord só pode vincular alunos a seus próprios cursos
            if (auth.profile() == UserProfile.COORDINATOR) {
                List<UUID> linked = ccRepo.findCourseIdsByCoordinatorId(auth.userId());
                if (!linked.contains(courseId)) {
                    throw new BusinessRuleException(ErrorCode.COORDINATOR_NOT_LINKED_TO_COURSE,
                            "Coordenador não está vinculado ao curso");
                }
            }
        }

        UUID id = Generators.timeBasedEpochGenerator().generate();
        Student s = Student.builder()
                .id(id)
                .registrationCode(req.registrationCode().trim())
                .name(req.name().trim())
                .email(req.email().trim().toLowerCase())
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        studentRepo.save(s);

        for (UUID courseId : req.courseIds()) {
            em.createNativeQuery(
                    "INSERT INTO student_course (student_id, course_id) VALUES (:sid, :cid)")
                    .setParameter("sid", id)
                    .setParameter("cid", courseId)
                    .executeUpdate();
        }

        auditService.recordEntityAction("CREATE_STUDENT", auth.userId(),
                EntityType.STUDENT, id, req.courseIds().isEmpty() ? null : req.courseIds().get(0));

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new StudentDto(s.getId(), s.getRegistrationCode(), s.getName(), s.getEmail(),
                        s.isActive(), req.courseIds(), s.getCreatedAt()));
    }
}
