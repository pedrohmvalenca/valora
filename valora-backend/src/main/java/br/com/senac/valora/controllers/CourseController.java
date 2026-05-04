package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CourseDto;
import br.com.senac.valora.dtos.CreateCourseRequest;
import br.com.senac.valora.entities.Course;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.AuditService;
import br.com.senac.valora.entities.EntityType;
import com.fasterxml.uuid.Generators;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Story Epic 2 — CRUD básico de cursos (Admin only). Tier B do batch γ.
 * Apenas LIST + CREATE nesta versão; edit/delete ficam para sprint futuro.
 */
@RestController
@RequestMapping("/api/v1/courses")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class CourseController {

    private final CourseRepository repo;
    private final AuditService auditService;

    public CourseController(CourseRepository repo, AuditService auditService) {
        this.repo = repo;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<CourseDto>> list() {
        List<CourseDto> all = repo.findAll().stream()
                .map(c -> new CourseDto(c.getId(), c.getName(), c.getCode(),
                        c.getMinimumWorkloadHours(), c.isActive(), c.getCreatedAt()))
                .toList();
        return ResponseEntity.ok(all);
    }

    @PostMapping
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CreateCourseRequest req,
                                            JwtAuthentication auth) {
        // Validação UNIQUE — preempt do constraint banco para mensagem clara
        if (repo.findAll().stream().anyMatch(c -> c.getCode().equalsIgnoreCase(req.code()))) {
            throw new BusinessRuleException(ErrorCode.DATA_INTEGRITY_VIOLATION,
                    "Já existe curso com código '" + req.code() + "'");
        }
        Course c = Course.builder()
                .id(Generators.timeBasedEpochGenerator().generate())
                .name(req.name())
                .code(req.code())
                .minimumWorkloadHours(req.minimumWorkloadHours() != null ? req.minimumWorkloadHours() : 100)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        repo.save(c);
        auditService.recordEntityAction("CREATE_COURSE", auth.userId(),
                EntityType.COURSE, c.getId(), c.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CourseDto(c.getId(), c.getName(), c.getCode(), c.getMinimumWorkloadHours(),
                        c.isActive(), c.getCreatedAt()));
    }
}
