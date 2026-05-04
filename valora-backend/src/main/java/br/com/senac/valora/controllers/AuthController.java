package br.com.senac.valora.controllers;

import br.com.senac.valora.dtos.LoginRequest;
import br.com.senac.valora.dtos.LoginResponse;
import br.com.senac.valora.dtos.MeResponse;
import br.com.senac.valora.dtos.UserDto;
import br.com.senac.valora.mappers.UserMapper;
import br.com.senac.valora.security.JwtAuthentication;
import br.com.senac.valora.services.AuditService;
import br.com.senac.valora.services.AuthService;
import br.com.senac.valora.services.AuthService.LoginSuccess;
import br.com.senac.valora.services.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String COOKIE_NAME = "AUTH_TOKEN";

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;
    private final boolean cookieSecure;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            UserMapper userMapper,
            AuditService auditService,
            @Value("${valora.jwt.cookie.secure}") boolean cookieSecure) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditService = auditService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletResponse response) {
        LoginSuccess result = authService.authenticate(req.email(), req.password());

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, result.token())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(jwtService.getExpirationSeconds())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        UserDto dto = userMapper.toDto(result.user(), result.linkedCourses());

        auditService.recordLogin(result.user().getId());

        return ResponseEntity.ok(new LoginResponse(dto));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        java.util.UUID userId = (auth instanceof JwtAuthentication jwt) ? jwt.userId() : null;

        ResponseCookie clearCookie = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, clearCookie.toString());

        auditService.recordLogout(userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeResponse> me(JwtAuthentication auth) {
        UserDto dto = authService.buildMeResponse(auth.userId());
        return ResponseEntity.ok(new MeResponse(dto));
    }
}
