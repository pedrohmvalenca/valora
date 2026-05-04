package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CategoryCourseLimitDto;
import br.com.senac.valora.dtos.CategoryDto;
import br.com.senac.valora.dtos.CreateCategoryRequest;
import br.com.senac.valora.entities.Category;
import br.com.senac.valora.entities.CategoryCourse;
import br.com.senac.valora.entities.EntityType;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CategoryCourseRepository;
import br.com.senac.valora.repositories.CategoryRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.AuditService;
import com.fasterxml.uuid.Generators;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class CategoryController {

    private final CategoryRepository catRepo;
    private final CategoryCourseRepository ccRepo;
    private final CourseRepository courseRepo;
    private final AuditService auditService;

    public CategoryController(CategoryRepository catRepo, CategoryCourseRepository ccRepo,
                              CourseRepository courseRepo, AuditService auditService) {
        this.catRepo = catRepo;
        this.ccRepo = ccRepo;
        this.courseRepo = courseRepo;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryDto>> list() {
        List<Category> all = catRepo.findAllByOrderByNameAsc();
        List<CategoryDto> result = all.stream().map(c -> {
            List<CategoryCourseLimitDto> limits = ccRepo.findByCategoryId(c.getId()).stream()
                    .map(cc -> new CategoryCourseLimitDto(cc.getCourseId(), cc.getMaxHours()))
                    .toList();
            return new CategoryDto(c.getId(), c.getName(), c.getGroupType(),
                    c.getDescription(), limits, c.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CategoryDto> create(@Valid @RequestBody CreateCategoryRequest req,
                                              JwtAuthentication auth) {
        // Validar cursos existem
        for (CategoryCourseLimitDto l : req.courseLimits()) {
            if (courseRepo.findById(l.courseId()).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.NOT_FOUND,
                        "Curso não encontrado: id=" + l.courseId());
            }
        }

        UUID id = Generators.timeBasedEpochGenerator().generate();
        Category c = Category.builder()
                .id(id)
                .name(req.name().trim())
                .groupType(req.groupType())
                .description(req.description())
                .createdAt(Instant.now())
                .build();
        catRepo.save(c);

        for (CategoryCourseLimitDto l : req.courseLimits()) {
            CategoryCourse cc = CategoryCourse.builder()
                    .categoryId(id)
                    .courseId(l.courseId())
                    .maxHours(l.maxHours())
                    .createdAt(Instant.now())
                    .build();
            ccRepo.save(cc);
        }

        auditService.recordEntityAction("CREATE_CATEGORY", auth.userId(),
                EntityType.CATEGORY, id, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CategoryDto(c.getId(), c.getName(), c.getGroupType(),
                        c.getDescription(), req.courseLimits(), c.getCreatedAt()));
    }
}
