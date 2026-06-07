package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CategoryCourseLimitDto;
import br.com.senac.valora.dtos.CategoryDto;
import br.com.senac.valora.dtos.CourseDto;
import br.com.senac.valora.dtos.MySubmissionDto;
import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.Course;
import br.com.senac.valora.entities.Submission;
import br.com.senac.valora.entities.SubmissionStatus;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.repositories.CategoryCourseRepository;
import br.com.senac.valora.repositories.CategoryRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.repositories.SubmissionRepository;
import br.com.senac.valora.security.JwtAuthentication;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@PreAuthorize("isAuthenticated()")
public class MeController {

    private final CourseRepository courseRepo;
    private final CategoryRepository categoryRepo;
    private final CategoryCourseRepository categoryCourseRepo;
    private final SubmissionRepository submissionRepo;

    @PersistenceContext
    private EntityManager em;

    public MeController(CourseRepository courseRepo, CategoryRepository categoryRepo,
                        CategoryCourseRepository categoryCourseRepo, SubmissionRepository submissionRepo) {
        this.courseRepo = courseRepo;
        this.categoryRepo = categoryRepo;
        this.categoryCourseRepo = categoryCourseRepo;
        this.submissionRepo = submissionRepo;
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
