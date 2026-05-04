package br.com.senac.valora.security;

import br.com.senac.valora.controllers.BaseSecuredController;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller dummy para testar combinações DEFAULT DENY + {@code RoleHierarchy}.
 *
 * <p>Registrado <b>apenas</b> no profile {@code test} via {@link Profile} —
 * NÃO sobe em local/homol/prod (anti-pattern explicitamente proibido pela AC1).
 *
 * <p>As rotas vivem em {@code /api/v1/_test/secured} para deixar evidente
 * que são test-only. Por estender {@link BaseSecuredController}, herda
 * {@code @PreAuthorize("denyAll()")} de classe — methods sem
 * {@code @PreAuthorize} próprio retornam 403 (DEFAULT DENY).
 */
@RestController
@RequestMapping("/api/v1/_test/secured")
@Profile("test")
public class DummySecuredController extends BaseSecuredController {

    @GetMapping("/unrestricted")
    public Map<String, String> unrestricted() {
        // Sem @PreAuthorize próprio → herda denyAll() da classe → 403
        return Map.of("ok", "true");
    }

    @GetMapping("/coordinator-only")
    @PreAuthorize("hasRole('COORDINATOR')")
    public Map<String, String> coordinatorOnly() {
        return Map.of("ok", "true");
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public Map<String, String> adminOnly() {
        return Map.of("ok", "true");
    }

    @GetMapping("/student-or-up")
    @PreAuthorize("hasRole('STUDENT')")
    public Map<String, String> studentOrUp() {
        return Map.of("ok", "true");
    }
}
