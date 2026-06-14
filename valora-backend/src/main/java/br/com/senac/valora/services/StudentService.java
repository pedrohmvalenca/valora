package br.com.senac.valora.services;

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
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private static final String SQL_COURSES_ADMIN =
            "SELECT sc.course_id, c.code, c.name, sc.status, sc.created_at "
            + "FROM student_course sc "
            + "JOIN courses c ON c.id = sc.course_id "
            + "WHERE sc.student_id = :sid "
            + "ORDER BY c.code ASC";

    private static final String SQL_COURSES_COORD =
            "SELECT sc.course_id, c.code, c.name, sc.status, sc.created_at "
            + "FROM student_course sc "
            + "JOIN courses c ON c.id = sc.course_id "
            + "WHERE sc.student_id = :sid "
            + "AND sc.course_id IN (SELECT course_id FROM coordinator_course WHERE coordinator_id = :cid) "
            + "ORDER BY c.code ASC";

    private final StudentRepository studentRepo;
    private final CourseRepository courseRepo;
    private final CoordinatorCourseRepository ccRepo;
    private final UserRepository userRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @PersistenceContext
    private EntityManager em;

    public StudentService(StudentRepository studentRepo, CourseRepository courseRepo,
                          CoordinatorCourseRepository ccRepo, UserRepository userRepo,
                          BCryptPasswordEncoder passwordEncoder, AuditService auditService) {
        this.studentRepo = studentRepo;
        this.courseRepo = courseRepo;
        this.ccRepo = ccRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<StudentDto> list(JwtAuthentication auth) {
        List<Student> students;
        if (auth.profile() == UserProfile.ADMINISTRATOR) {
            students = studentRepo.findAll();
        } else {
            List<UUID> linked = ccRepo.findCourseIdsByCoordinatorId(auth.userId());
            students = linked.isEmpty() ? List.of() : studentRepo.findAllInCourses(linked);
        }
        List<StudentDto> result = new ArrayList<>();
        for (Student s : students) {
            List<UUID> courseIds = fetchCourseIdsForStudent(s.getId());
            result.add(new StudentDto(s.getId(), s.getRegistrationCode(), s.getName(), s.getEmail(),
                    s.isActive(), courseIds, s.getCreatedAt(), null));
        }
        return result;
    }

    @Transactional
    public StudentDto create(CreateStudentRequest req, JwtAuthentication auth) {
        List<UUID> coordLinked = auth.profile() == UserProfile.COORDINATOR
                ? ccRepo.findCourseIdsByCoordinatorId(auth.userId())
                : null;

        for (UUID courseId : req.courseIds()) {
            if (courseRepo.findById(courseId).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.NOT_FOUND,
                        "Curso não encontrado: id=" + courseId);
            }
            if (coordLinked != null && !coordLinked.contains(courseId)) {
                throw new BusinessRuleException(ErrorCode.COORDINATOR_NOT_LINKED_TO_COURSE,
                        "Coordenador não está vinculado ao curso");
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

        return new StudentDto(s.getId(), s.getRegistrationCode(), s.getName(), s.getEmail(),
                s.isActive(), req.courseIds(), s.getCreatedAt(),
                generatedPassword ? rawPassword : null);
    }

    @Transactional(readOnly = true)
    public List<StudentSearchResultDto> search(String q) {
        String term = q == null ? "" : q.trim();
        if (term.length() < 2) {
            return List.of();
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
        List<StudentSearchResultDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new StudentSearchResultDto(
                    (UUID) r[0],
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    (Boolean) r[4],
                    ((Number) r[5]).intValue()));
        }
        return result;
    }

    @Transactional
    public StudentDto linkCourses(UUID studentId, LinkStudentCoursesRequest req, JwtAuthentication auth) {
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

        List<UUID> courseIds = fetchCourseIdsForStudent(studentId);
        return new StudentDto(s.getId(), s.getRegistrationCode(), s.getName(),
                s.getEmail(), s.isActive(), courseIds, s.getCreatedAt(), null);
    }

    @Transactional(readOnly = true)
    public List<StudentCourseStatusDto> listCoursesWithStatus(UUID studentId, JwtAuthentication auth) {
        if (studentRepo.findById(studentId).isEmpty()) {
            throw new EntityNotFoundException("Aluno não encontrado: id=" + studentId);
        }

        Query query;
        if (auth.profile() == UserProfile.COORDINATOR) {
            query = em.createNativeQuery(SQL_COURSES_COORD)
                    .setParameter("sid", studentId)
                    .setParameter("cid", auth.userId());
        } else {
            query = em.createNativeQuery(SQL_COURSES_ADMIN)
                    .setParameter("sid", studentId);
        }

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<StudentCourseStatusDto> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new StudentCourseStatusDto(
                    (UUID) r[0],
                    (String) r[1],
                    (String) r[2],
                    (String) r[3],
                    asInstant(r[4])));
        }
        return result;
    }

    @Transactional
    public StudentCourseStatusDto updateCourseStatus(UUID studentId, UUID courseId,
                                                     UpdateStudentCourseStatusRequest req,
                                                     JwtAuthentication auth) {
        if (auth.profile() == UserProfile.COORDINATOR) {
            List<UUID> linked = ccRepo.findCourseIdsByCoordinatorId(auth.userId());
            if (!linked.contains(courseId)) {
                throw new BusinessRuleException(ErrorCode.COORDINATOR_NOT_LINKED_TO_COURSE,
                        "Coordenador não está vinculado ao curso");
            }
        }

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

        if (currentStatus.equals(req.status())) {
            return new StudentCourseStatusDto(
                    courseId, courseCode, courseName, currentStatus, createdAt);
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

        return new StudentCourseStatusDto(
                courseId, courseCode, courseName, req.status(), createdAt);
    }

    private List<UUID> fetchCourseIdsForStudent(UUID studentId) {
        @SuppressWarnings("unchecked")
        List<Object> raw = em.createNativeQuery(
                "SELECT course_id FROM student_course WHERE student_id = :sid")
                .setParameter("sid", studentId)
                .getResultList();
        List<UUID> ids = new ArrayList<>();
        for (Object o : raw) {
            ids.add((UUID) o);
        }
        return ids;
    }

    private static String generateProvisionalPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Instant asInstant(Object o) {
        if (o == null) return null;
        if (o instanceof Instant i) return i;
        if (o instanceof Timestamp ts) return ts.toInstant();
        if (o instanceof OffsetDateTime odt) return odt.toInstant();
        throw new IllegalStateException("Tipo inesperado para timestamp: " + o.getClass());
    }
}
