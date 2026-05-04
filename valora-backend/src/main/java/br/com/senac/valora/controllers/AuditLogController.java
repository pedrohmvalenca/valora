package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.AuditLogDto;
import br.com.senac.valora.entities.AuditLog;
import br.com.senac.valora.entities.EntityType;
import br.com.senac.valora.repositories.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Story 1.8 — consulta da trilha de auditoria. Admin only.
 *
 * <p>Filtros opcionais via query params. Sem retornar {@code payloadJson}
 * (privacidade — pode conter dados sensíveis em versões futuras).
 */
@RestController
@RequestMapping("/api/v1/logs")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AuditLogController {

    private final AuditLogRepository repo;

    public AuditLogController(AuditLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLogDto>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) EntityType entityType,
            Pageable pageable) {

        Page<AuditLog> page;
        if (action != null && entityType != null) {
            page = repo.findByActionAndEntityTypeOrderByCreatedAtDesc(action, entityType, pageable);
        } else if (action != null) {
            page = repo.findByActionOrderByCreatedAtDesc(action, pageable);
        } else if (entityType != null) {
            page = repo.findByEntityTypeOrderByCreatedAtDesc(entityType, pageable);
        } else {
            page = repo.findAllByOrderByCreatedAtDesc(pageable);
        }
        return ResponseEntity.ok(page.map(this::toDto));
    }

    private AuditLogDto toDto(AuditLog l) {
        return new AuditLogDto(l.getId(), l.getUserId(), l.getAction(),
                l.getEntityType(), l.getEntityId(), l.getCourseId(), l.getCreatedAt());
    }
}
