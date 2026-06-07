package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.BalanceDto;
import br.com.senac.valora.dtos.CategoryCourseLimitDto;
import br.com.senac.valora.dtos.CategoryDto;
import br.com.senac.valora.dtos.CourseDto;
import br.com.senac.valora.dtos.DashboardCategoryDto;
import br.com.senac.valora.dtos.DashboardCourseDto;
import br.com.senac.valora.dtos.MySubmissionDto;
import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.CategoryCourse;
import br.com.senac.valora.entities.Course;
import br.com.senac.valora.entities.Student;
import br.com.senac.valora.entities.Submission;
import br.com.senac.valora.entities.SubmissionStatus;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CategoryCourseRepository;
import br.com.senac.valora.repositories.CategoryRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.repositories.StudentRepository;
import br.com.senac.valora.repositories.SubmissionRepository;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.MailService;
import com.fasterxml.uuid.Generators;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final CourseRepository courseRepo;
    private final CategoryRepository categoryRepo;
    private final CategoryCourseRepository categoryCourseRepo;
    private final SubmissionRepository submissionRepo;
    private final StudentRepository studentRepo;
    private final MailService mailService;

    @PersistenceContext
    private EntityManager em;

    public MeController(CourseRepository courseRepo, CategoryRepository categoryRepo,
                        CategoryCourseRepository categoryCourseRepo, SubmissionRepository submissionRepo,
                        StudentRepository studentRepo, MailService mailService) {
        this.courseRepo = courseRepo;
        this.categoryRepo = categoryRepo;
        this.categoryCourseRepo = categoryCourseRepo;
        this.submissionRepo = submissionRepo;
        this.studentRepo = studentRepo;
        this.mailService = mailService;
    }

    @GetMapping("/courses")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CourseDto>> myCourses(JwtAuthentication auth) {
        UUID studentId = requireStudent(auth);
        List<CourseDto> result = courseRepo.findAllById(linkedCourseIds(studentId)).stream()
                .map(c -> new CourseDto(c.getId(), c.getName(), c.getCode(),
                        c.getMinimumWorkloadHours(), c.isActive(), c.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/categories")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CategoryDto>> myCategories(@RequestParam UUID courseId, JwtAuthentication auth) {
        UUID studentId = requireStudent(auth);
        if (!linkedCourseIds(studentId).contains(courseId)) {
            return ResponseEntity.ok(List.of());
        }
        List<UUID> categoryIds = categoryCourseRepo.findByCourseId(courseId).stream()
                .map(cc -> cc.getCategoryId())
                .toList();
        List<CategoryDto> result = categoryRepo.findAllById(categoryIds).stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .map(c -> {
                    List<CategoryCourseLimitDto> limits = categoryCourseRepo.findByCategoryId(c.getId()).stream()
                            .map(cc -> new CategoryCourseLimitDto(cc.getCourseId(), cc.getMaxHours()))
                            .toList();
                    return new CategoryDto(c.getId(), c.getName(), c.getGroupType(),
                            c.getDescription(), limits, c.getCreatedAt());
                })
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/submissions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<MySubmissionDto>> mySubmissions(
            @RequestParam(required = false) SubmissionStatus status, JwtAuthentication auth) {
        UUID studentId = requireStudent(auth);
        List<Submission> subs = status != null
                ? submissionRepo.findByStudentIdAndStatusOrderByCreatedAtDesc(studentId, status)
                : submissionRepo.findByStudentIdOrderByCreatedAtDesc(studentId);

        Map<UUID, Course> courses = courseRepo
                .findAllById(subs.stream().map(Submission::getCourseId).distinct().toList())
                .stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        Map<UUID, Category> categories = categoryRepo
                .findAllById(subs.stream().map(Submission::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        List<MySubmissionDto> result = subs.stream().map(s -> {
            Course c = courses.get(s.getCourseId());
            Category cat = categories.get(s.getCategoryId());
            return new MySubmissionDto(
                    s.getId(), s.getCourseId(), c != null ? c.getName() : null,
                    s.getCategoryId(), cat != null ? cat.getName() : null,
                    cat != null ? cat.getGroupType() : null,
                    s.getDescription(), s.getRequestedHours(), s.getRecognizedHours(),
                    s.getStatus(), s.getRejectionReason(), s.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public ResponseEntity<List<DashboardCourseDto>> myDashboard(JwtAuthentication auth) {
        UUID studentId = requireStudent(auth);
        List<DashboardCourseDto> result = courseRepo.findAllById(linkedCourseIds(studentId)).stream()
                .map(course -> buildCourseDashboard(studentId, course))
                .toList();
        return ResponseEntity.ok(result);
    }

    private DashboardCourseDto buildCourseDashboard(UUID studentId, Course course) {
        UUID courseId = course.getId();
        List<CategoryCourse> links = categoryCourseRepo.findByCourseId(courseId);
        Map<UUID, Category> catMap = categoryRepo
                .findAllById(links.stream().map(CategoryCourse::getCategoryId).toList())
                .stream().collect(Collectors.toMap(Category::getId, Function.identity()));

        List<DashboardCategoryDto> categories = new ArrayList<>();
        int totalRecognized = 0;
        for (CategoryCourse link : links) {
            UUID categoryId = link.getCategoryId();
            Category cat = catMap.get(categoryId);
            int max = link.getMaxHours();
            int accumulated = submissionRepo.sumApprovedHours(studentId, courseId, categoryId);
            int remaining = Math.max(0, max - accumulated);
            totalRecognized += accumulated;
            categories.add(new DashboardCategoryDto(categoryId,
                    cat != null ? cat.getName() : null,
                    cat != null ? cat.getGroupType() : null,
                    new BalanceDto(max, accumulated, remaining)));
        }
        categories.sort(Comparator.comparing(c -> c.name() == null ? "" : c.name(), String.CASE_INSENSITIVE_ORDER));

        int minHours = course.getMinimumWorkloadHours();
        int progress = minHours > 0 ? Math.min(100, (int) Math.round(totalRecognized * 100.0 / minHours)) : 0;
        int pending = (int) submissionRepo.countByStudentIdAndCourseIdAndStatus(
                studentId, courseId, SubmissionStatus.PENDING);

        return new DashboardCourseDto(courseId, course.getName(), minHours,
                totalRecognized, progress, pending, categories);
    }

    @PostMapping(value = "/submissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<MySubmissionDto> createSubmission(
            @RequestParam UUID courseId,
            @RequestParam UUID categoryId,
            @RequestParam(required = false, defaultValue = "") String description,
            @RequestParam int requestedHours,
            @RequestParam("proof") MultipartFile proof,
            JwtAuthentication auth) {
        UUID studentId = requireStudent(auth);

        if (!linkedCourseIds(studentId).contains(courseId)) {
            throw new EntityNotFoundException("Curso não vinculado ao aluno");
        }
        if (categoryCourseRepo.findByCategoryIdAndCourseId(categoryId, courseId).isEmpty()) {
            throw new EntityNotFoundException("Categoria não disponível para o curso");
        }
        if (requestedHours <= 0) {
            throw new BusinessRuleException(ErrorCode.VALIDATION_ERROR,
                    "Horas solicitadas devem ser maiores que zero");
        }
        validateProof(proof);
        if (submissionRepo.existsByStudentIdAndCategoryIdAndStatus(studentId, categoryId, SubmissionStatus.PENDING)) {
            throw new BusinessRuleException(ErrorCode.DUPLICATE_PENDING_SUBMISSION,
                    "Já existe uma submissão pendente nesta categoria");
        }

        byte[] data;
        try {
            data = proof.getBytes();
        } catch (IOException e) {
            throw new BusinessRuleException(ErrorCode.PROOF_REQUIRED, "Falha ao ler o comprovante");
        }

        UUID id = Generators.timeBasedEpochGenerator().generate();
        Submission s = Submission.builder()
                .id(id)
                .studentId(studentId)
                .courseId(courseId)
                .categoryId(categoryId)
                .description(description == null ? "" : description.trim())
                .requestedHours(requestedHours)
                .status(SubmissionStatus.PENDING)
                .proofData(data)
                .proofContentType(proof.getContentType())
                .createdAt(Instant.now())
                .build();
        submissionRepo.save(s);

        Course c = courseRepo.findById(courseId).orElse(null);
        Category cat = categoryRepo.findById(categoryId).orElse(null);
        MySubmissionDto dto = new MySubmissionDto(
                s.getId(), courseId, c != null ? c.getName() : null,
                categoryId, cat != null ? cat.getName() : null, cat != null ? cat.getGroupType() : null,
                s.getDescription(), s.getRequestedHours(), s.getRecognizedHours(),
                s.getStatus(), s.getRejectionReason(), s.getCreatedAt());

        Student student = studentRepo.findById(studentId).orElse(null);
        if (student != null) {
            mailService.sendSubmissionConfirmation(student.getEmail(), student.getName(),
                    c != null ? c.getName() : "", cat != null ? cat.getName() : "", s.getRequestedHours());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    private static final long MAX_PROOF_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_PROOF_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");

    private void validateProof(MultipartFile proof) {
        if (proof == null || proof.isEmpty()) {
            throw new BusinessRuleException(ErrorCode.PROOF_REQUIRED, "Comprovante é obrigatório");
        }
        if (proof.getSize() > MAX_PROOF_BYTES) {
            throw new BusinessRuleException(ErrorCode.PROOF_REQUIRED, "Comprovante excede 5MB");
        }
        String contentType = proof.getContentType();
        if (contentType == null || !ALLOWED_PROOF_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException(ErrorCode.PROOF_REQUIRED, "Comprovante deve ser PDF, JPG ou PNG");
        }
    }

    private UUID requireStudent(JwtAuthentication auth) {
        if (auth.profile() != UserProfile.STUDENT) {
            throw new AccessDeniedException("Recurso exclusivo do aluno");
        }
        return auth.userId();
    }

    @SuppressWarnings("unchecked")
    private List<UUID> linkedCourseIds(UUID studentId) {
        return em.createNativeQuery("SELECT course_id FROM student_course WHERE student_id = :sid")
                .setParameter("sid", studentId)
                .getResultList().stream().map(o -> (UUID) o).toList();
    }
}
