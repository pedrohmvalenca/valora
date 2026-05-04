package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.ApproveSubmissionRequest;
import br.com.senac.valora.dtos.RejectSubmissionRequest;
import br.com.senac.valora.dtos.SubmissionDetailDto;
import br.com.senac.valora.dtos.SubmissionListItemDto;
import br.com.senac.valora.entities.SubmissionStatus;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.SubmissionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/submissions")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class SubmissionController {

    private final SubmissionService service;

    public SubmissionController(SubmissionService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<SubmissionListItemDto>> list(
            @RequestParam(required = false) SubmissionStatus status,
            @RequestParam(required = false) UUID courseId,
            Pageable pageable,
            JwtAuthentication auth) {
        return ResponseEntity.ok(service.list(auth, status, courseId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionDetailDto> detail(@PathVariable UUID id, JwtAuthentication auth) {
        return ResponseEntity.ok(service.getDetail(id, auth));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApproveSubmissionRequest req,
            JwtAuthentication auth) {
        service.approve(id, req, auth);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/revert")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> revert(@PathVariable UUID id, JwtAuthentication auth) {
        service.revertDecision(id, auth);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectSubmissionRequest req,
            JwtAuthentication auth) {
        service.reject(id, req, auth);
        return ResponseEntity.ok().build();
    }
}
