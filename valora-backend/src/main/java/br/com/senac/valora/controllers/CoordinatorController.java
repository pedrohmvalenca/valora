package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.CoordinatorDto;
import br.com.senac.valora.dtos.CreateCoordinatorRequest;
import br.com.senac.valora.entities.CoordinatorCourse;
import br.com.senac.valora.entities.EntityType;
import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.exceptions.BusinessRuleException;
import br.com.senac.valora.exceptions.ErrorCode;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.CourseRepository;
import br.com.senac.valora.repositories.UserRepository;
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
@RequestMapping("/api/v1/coordinators")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class CoordinatorController {

    private static final String DEFAULT_PASSWORD_HASH =
            "$2b$12$9qXNdy6QXExGNlC0Gkecoua59XBL6XrKAr8aFdOnGijDqNBjp28K2";  // = Admin@123

    private final UserRepository userRepo;
    private final CourseRepository courseRepo;
    private final CoordinatorCourseRepository ccRepo;
    private final AuditService auditService;

    public CoordinatorController(UserRepository userRepo, CourseRepository courseRepo,
                                 CoordinatorCourseRepository ccRepo, AuditService auditService) {
        this.userRepo = userRepo;
        this.courseRepo = courseRepo;
        this.ccRepo = ccRepo;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<CoordinatorDto>> list() {
        List<User> coords = userRepo.findByProfileOrderByNameAsc(UserProfile.COORDINATOR);
        List<CoordinatorDto> result = coords.stream().map(u -> {
            List<UUID> courses = ccRepo.findCourseIdsByCoordinatorId(u.getId());
            return new CoordinatorDto(u.getId(), u.getName(), u.getEmail(),
                    u.isActive(), courses, u.getCreatedAt());
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CoordinatorDto> create(@Valid @RequestBody CreateCoordinatorRequest req,
                                                 JwtAuthentication auth) {
        String email = req.email().trim().toLowerCase();
        if (userRepo.findByEmail(email).isPresent()) {
            throw new BusinessRuleException(ErrorCode.DATA_INTEGRITY_VIOLATION,
                    "Já existe usuário com email '" + email + "'");
        }
        // Validar que todos os cursos existem
        for (UUID courseId : req.courseIds()) {
            if (courseRepo.findById(courseId).isEmpty()) {
                throw new BusinessRuleException(ErrorCode.NOT_FOUND,
                        "Curso não encontrado: id=" + courseId);
            }
        }

        UUID id = Generators.timeBasedEpochGenerator().generate();
        User u = User.builder()
                .id(id)
                .email(email)
                .passwordHash(DEFAULT_PASSWORD_HASH)
                .name(req.name())
                .profile(UserProfile.COORDINATOR)
                .isActive(true)
                .createdAt(Instant.now())
                .build();
        userRepo.save(u);

        // Vincula cursos
        for (UUID courseId : req.courseIds()) {
            CoordinatorCourse cc = CoordinatorCourse.builder()
                    .id(new br.com.senac.valora.entities.CoordinatorCourseId(id, courseId))
                    .createdAt(Instant.now())
                    .build();
            ccRepo.save(cc);
        }

        auditService.recordEntityAction("CREATE_COORDINATOR", auth.userId(),
                EntityType.COORDINATOR, id, null);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new CoordinatorDto(u.getId(), u.getName(), u.getEmail(), u.isActive(),
                        req.courseIds(), u.getCreatedAt()));
    }
}
