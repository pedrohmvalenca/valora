package br.com.senac.valora.services;

import br.com.senac.valora.entities.AuditLog;
import br.com.senac.valora.entities.EntityType;
import br.com.senac.valora.repositories.AuditLogRepository;
import com.fasterxml.uuid.Generators;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repo;

    public AuditService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Async("auditExecutor")
    public void recordLogin(UUID userId) {
        persist("LOGIN", userId, EntityType.USER, userId, null);
    }

    @Async("auditExecutor")
    public void recordLogout(UUID userId) {
        // userId pode ser null (logout idempotente sem cookie) — log audit anônimo
        if (userId == null) {
            log.debug("Logout sem userId conhecido (cookie ausente/expirado) — audit pulado");
            return;
        }
        persist("LOGOUT", userId, EntityType.USER, userId, null);
    }

    @Async("auditExecutor")
    public void recordSubmissionDecision(String action, UUID userId, UUID submissionId, UUID courseId) {
        persist(action, userId, EntityType.SUBMISSION, submissionId, courseId);
    }

    @Async("auditExecutor")
    public void recordEntityAction(String action, UUID userId, EntityType entityType,
                                   UUID entityId, UUID courseId) {
        persist(action, userId, entityType, entityId, courseId);
    }

    private void persist(String action, UUID userId, EntityType entityType, UUID entityId, UUID courseId) {
        try {
            AuditLog entry = AuditLog.builder()
                    .id(Generators.timeBasedEpochGenerator().generate())  // UUID v7 (D3)
                    .userId(userId)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .courseId(courseId)
                    .createdAt(Instant.now())
                    .build();
            repo.save(entry);
        } catch (Exception ex) {
            // Audit nunca derruba o request — apenas loga
            log.warn("Falha ao persistir audit log (action={}, userId={}): {}", action, userId, ex.getMessage());
        }
    }
}
