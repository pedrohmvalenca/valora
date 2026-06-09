package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CreateStudentRequest;
import br.com.senac.valora.dtos.LinkStudentCoursesRequest;
import br.com.senac.valora.dtos.StudentCourseStatusDto;
import br.com.senac.valora.dtos.StudentDto;
import br.com.senac.valora.dtos.StudentSearchResultDto;
import br.com.senac.valora.dtos.UpdateStudentCourseStatusRequest;
import br.com.senac.valora.entities.EntityType;
import br.com.senac.valora.entities.Student;
import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.repositories.StudentRepository;
import br.com.senac.valora.repositories.UserRepository;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.AuditService;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.validation.Valid;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * Story Epic 3 (γ) — CRUD de alunos (Coord vê só vinculados, Admin vê tudo).
 *
 * Story 5.1: criar um aluno também cria um User(profile=STUDENT) com o mesmo id,
 * e-mail e senha provisória (BCrypt), habilitando o login do app mobile.
 */
@RestController
@RequestMapping("/api/v1/students")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class StudentController {

    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;
    private final CoordinatorCourseRepository ccRepo;
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager em;

    public StudentController(StudentRepository studentRepo, CourseRepository courseRepo,
                             CoordinatorCourseRepository ccRepo, UserRepository userRepo,
                             BCryptPasswordEncoder passwordEncoder, AuditService auditService) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
        this.ccRepo = ccRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
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
                    s.isActive(), courseIds, s.getCreatedAt(), null);
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

        String email = req.email().trim().toLowerCase();
        if (userRepo.findByEmail(email).isPresent()) {
            throw new BusinessRuleException(ErrorCode.DATA_INTEGRITY_VIOLATION,
                    "E-mail já está em uso: " + email);
        }

        boolean generatedPassword = req.password() == null || req.password().isBlank();
        String rawPassword = generatedPassword ? generateProvisionalPassword() : req.password();

        UUID id = Generators.timeBasedEpochGenerator().generate();
        Instant now = Instant.now();
        Student s = Student.builder()
                .id(id)
                .registrationCode(req.registrationCode().trim())
                .name(req.name().trim())
                .email(email)
                .isActive(true)
                .createdAt(now)
                .build();
        studentRepo.save(s);

        User user = User.builder()
                .id(id)
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name(s.getName())
                .profile(UserProfile.STUDENT)
                .isActive(true)
                .createdAt(now)
                // Story 1.11: aluno nasce com mustChangePassword=true para forçar
                // troca da provisória no primeiro acesso. Se o cadastro usou senha
                // custom (req.password != null), também forçamos — não tem como
                // distinguir "Coord digitou a definitiva" de "Coord digitou outra
                // provisória", e o custo extra para o usuário é uma tela só.
                .mustChangePassword(true)
                .build();
        userRepo.save(user);

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
                        s.isActive(), req.courseIds(), s.getCreatedAt(),
                        generatedPassword ? rawPassword : null));
    }

    @GetMapping("/search")
    public ResponseEntity<List<StudentSearchResultDto>> search(@RequestParam("q") String q) {
        String term = q == null ? "" : q.trim();
        if (term.length() < 2) {
            return ResponseEntity.ok(List.of());
        }
        String like = "%" + term + "%";
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT s.id, s.registration_code, s.name, s.email, s.is_active, "
                + "(SELECT COUNT(*) FROM student_course sc WHERE sc.student_id = s.id) "
                + "FROM students s "
                + "WHERE s.name ILIKE :q OR s.email ILIKE :q OR s.registration_code ILIKE :q "
                + "ORDER BY s.name ASC LIMIT 20")
                .setParameter("q", like)
                .getResultList();
        List<StudentSearchResultDto> result = rows.stream()
                .map(r -> new StudentSearchResultDto(
                        (UUID) r[0],
                        (String) r[1],
                        (String) r[2],
                        (String) r[3],
                        (Boolean) r[4],
                        ((Number) r[5]).intValue()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{studentId}/courses")
    @Transactional
    public ResponseEntity<StudentDto> linkCourses(@PathVariable UUID studentId,
                                                  @Valid @RequestBody LinkStudentCoursesRequest req,
                                                  JwtAuthentication auth) {
        Student s = studentRepo.findById(studentId).orElseThrow(() ->
                new BusinessRuleException(ErrorCode.NOT_FOUND, "Aluno não encontrado: id=" + studentId));

        List<UUID> linked = auth.profile() == UserProfile.COORDINATOR
                ? ccRepo.findCourseIdsByCoordinatorId(auth.userId())
                : null;

        for (UUID courseId : req.courseIds()) {
            if (courseRepo.findById(courseId).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.NOT_FOUND,
                        "Curso não encontrado: id=" + courseId);
            }
            if (linked != null && !linked.contains(courseId)) {
                throw new BusinessRuleException(ErrorCode.COORDINATOR_NOT_LINKED_TO_COURSE,
                        "Coordenador não está vinculado ao curso");
            }
            em.createNativeQuery(
                    "INSERT INTO student_course (student_id, course_id) VALUES (:sid, :cid) "
                    + "ON CONFLICT (student_id, course_id) DO NOTHING")
                    .setParameter("sid", studentId)
                    .setParameter("cid", courseId)
                    .executeUpdate();
        }

        auditService.recordEntityAction("LINK_STUDENT_COURSE", auth.userId(),
                EntityType.STUDENT, studentId, req.courseIds().get(0));

        @SuppressWarnings("unchecked")
        List<UUID> courseIds = em.createNativeQuery(
                "SELECT course_id FROM student_course WHERE student_id = :sid")
                .setParameter("sid", studentId)
                .getResultList()
                .stream()
                .map(o -> (UUID) o)
                .toList();

        return ResponseEntity.ok(new StudentDto(s.getId(), s.getRegistrationCode(), s.getName(),
                s.getEmail(), s.isActive(), courseIds, s.getCreatedAt(), null));
    }

    /**
     * Story 3.10 — Lista enriquecida dos vínculos do aluno × cursos com {@code status}.
     *
     * <p>Endpoint NOVO; o contrato de {@code GET /api/v1/students} (linkedCourseIds)
     * permanece intacto para não regredir a Story 3.9. A migração desse contrato
     * para incluir status fica para a Story 3.11.
     *
     * <p>Coord só enxerga os vínculos do aluno que estão nos cursos vinculados a ele
     * (RN-0001 — não vaza nome de curso de outro coord).
     */
    @GetMapping("/{studentId}/courses")
    public ResponseEntity<List<StudentCourseStatusDto>> listCoursesWithStatus(
            @PathVariable UUID studentId, JwtAuthentication auth) {
        if (studentRepo.findById(studentId).isEmpty()) {
            throw new EntityNotFoundException("Aluno não encontrado: id=" + studentId);
        }

        String sql = "SELECT sc.course_id, c.code, c.name, sc.status, sc.created_at "
                + "FROM student_course sc "
                + "JOIN courses c ON c.id = sc.course_id "
                + "WHERE sc.student_id = :sid "
                + (auth.profile() == UserProfile.COORDINATOR
                        ? "AND sc.course_id IN (SELECT course_id FROM coordinator_course WHERE coordinator_id = :cid) "
                        : "")
                + "ORDER BY c.code ASC";

        var query = em.createNativeQuery(sql).setParameter("sid", studentId);
        if (auth.profile() == UserProfile.COORDINATOR) {
            query.setParameter("cid", auth.userId());
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<StudentCourseStatusDto> result = rows.stream()
                .map(r -> new StudentCourseStatusDto(
                        (UUID) r[0],
                        (String) r[1],
                        (String) r[2],
                        (String) r[3],
                        asInstant(r[4])))
                .toList();
        return ResponseEntity.ok(result);
    }

    /**
     * Story 3.10 — Muda o {@code status} de um vínculo aluno × curso.
     *
     * <p>Coord só pode mexer nos próprios cursos (RN-0001 → 409). Linkage
     * inexistente → 404. Valor inválido → 400 (via {@code @Pattern} no DTO).
     * Idempotente: mudar para o mesmo status não falha e não gera audit ruidoso.
     *
     * <p>Audit: {@code payloadJson} (from/to) é desejável (spec da story) mas
     * a entity {@code AuditLog} atual NÃO mapeia esse campo (TODO da Story 1.8.x).
     * Por isso registramos só action+entity+course; quando o payloadJson chegar,
     * a chamada aqui é trocada sem afetar contrato externo.
     */
    @PatchMapping("/{studentId}/courses/{courseId}/status")
    @Transactional
    public ResponseEntity<StudentCourseStatusDto> updateCourseStatus(
            @PathVariable UUID studentId,
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateStudentCourseStatusRequest req,
            JwtAuthentication auth) {

        // Coord: bloquear curso fora do escopo antes de tudo (RN-0001).
        if (auth.profile() == UserProfile.COORDINATOR) {
            List<UUID> linked = ccRepo.findCourseIdsByCoordinatorId(auth.userId());
            if (!linked.contains(courseId)) {
                throw new BusinessRuleException(ErrorCode.COORDINATOR_NOT_LINKED_TO_COURSE,
                        "Coordenador não está vinculado ao curso");
            }
        }

        // Buscar status atual + dados do curso. Se não há linkage, é 404.
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(
                "SELECT sc.status, c.code, c.name, sc.created_at "
                + "FROM student_course sc JOIN courses c ON c.id = sc.course_id "
                + "WHERE sc.student_id = :sid AND sc.course_id = :cid")
                .setParameter("sid", studentId)
                .setParameter("cid", courseId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new EntityNotFoundException(
                    "Vínculo não encontrado: aluno=" + studentId + ", curso=" + courseId);
        }
        Object[] row = rows.get(0);
        String currentStatus = (String) row[0];
        String courseCode = (String) row[1];
        String courseName = (String) row[2];
        Instant createdAt = asInstant(row[3]);

        // Idempotente: mesmo status → 200 sem update nem audit.
        if (currentStatus.equals(req.status())) {
            return ResponseEntity.ok(new StudentCourseStatusDto(
                    courseId, courseCode, courseName, currentStatus, createdAt));
        }

        em.createNativeQuery(
                "UPDATE student_course SET status = :s "
                + "WHERE student_id = :sid AND course_id = :cid")
                .setParameter("s", req.status())
                .setParameter("sid", studentId)
                .setParameter("cid", courseId)
                .executeUpdate();

        auditService.recordEntityAction("UPDATE_STUDENT_COURSE_STATUS", auth.userId(),
                EntityType.STUDENT, studentId, courseId);

        return ResponseEntity.ok(new StudentCourseStatusDto(
                courseId, courseCode, courseName, req.status(), createdAt));
    }

    private static String generateProvisionalPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Conversor defensivo: o JDBC do Postgres pode devolver {@code created_at}
     * (TIMESTAMP WITH TIME ZONE) como {@code Instant}, {@code OffsetDateTime}
     * ou {@code Timestamp} dependendo do driver/configuração. Centralizar aqui
     * evita repetir o branch em cada native query nova.
     */
    private static Instant asInstant(Object o) {
        if (o == null) return null;
        if (o instanceof Instant i) return i;
        if (o instanceof Timestamp ts) return ts.toInstant();
        if (o instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        throw new IllegalStateException("Tipo inesperado para timestamp: " + o.getClass());
    }
}
