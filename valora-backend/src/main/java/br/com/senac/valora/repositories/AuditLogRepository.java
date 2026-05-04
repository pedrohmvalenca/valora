package br.com.senac.valora.repositories;

import br.com.senac.valora.entities.AuditLog;
import br.com.senac.valora.entities.EntityType;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    Page<AuditLog> findByEntityTypeOrderByCreatedAtDesc(EntityType entityType, Pageable pageable);

    Page<AuditLog> findByActionAndEntityTypeOrderByCreatedAtDesc(
            String action, EntityType entityType, Pageable pageable);
}
