package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.DashboardCoordinatorItemDto;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.DashboardService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMINISTRATOR')")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/coordinator")
    public ResponseEntity<List<DashboardCoordinatorItemDto>> coordinator(
            @RequestParam(required = false) UUID courseId,
            JwtAuthentication auth) {
        return ResponseEntity.ok(service.coordinatorDashboard(auth, courseId));
    }
}
