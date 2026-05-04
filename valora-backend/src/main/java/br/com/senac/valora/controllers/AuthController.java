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

/**
 * Endpoints de autenticação.
 *
 * <p>{@code POST /api/v1/auth/login} — consome credenciais e emite o cookie
 * httpOnly {@code AUTH_TOKEN}. {@code GET /api/v1/auth/me} — devolve o usuário
 * a partir do JWT já validado pelo {@link br.com.senac.valora.security.JwtFilter}.
 *
 * <p><b>Caminho de erro centralizado:</b> {@link AuthService#authenticate} lança
 * {@code BadCredentialsException} em qualquer falha; o {@code GlobalExceptionHandler}
 * converte em 401 + {@code AUTH_001} + {@code "Credenciais inválidas"}. O controller
 * só lida com o caminho ok — eliminamos a 4ª via paralela de construção de
 * {@code ErrorResponse} que existia até a Story 1.4 v0.2.
 */
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
        // Falha em qualquer etapa joga BadCredentialsException — handler global
        // emite 401 + AUTH_001. Aqui só o caminho ok.
        LoginSuccess result = authService.authenticate(req.email(), req.password());

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, result.token())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/")
                .maxAge(jwtService.getExpirationSeconds())
                .build();
        // Sem setar atributo Domain — deixa o browser usar o host de origem.
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        UserDto dto = userMapper.toDto(result.user(), result.linkedCourses());

        // Audit (Story 1.8) — assíncrono, não bloqueia resposta
        auditService.recordLogin(result.user().getId());

        return ResponseEntity.ok(new LoginResponse(dto));
    }

    /**
     * Encerra sessão revogando o cookie {@code AUTH_TOKEN} via {@code Set-Cookie}
     * com {@code Max-Age=0}. Story 1.6 (modo γ).
     *
     * <p><b>Idempotente</b> — chamada SEM cookie (ou com cookie expirado) também
     * responde 200 com mesmo Set-Cookie limpando. Sem {@code @PreAuthorize} —
     * usuário deve sempre conseguir "sair", mesmo com sessão já corrompida.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        // Captura userId ANTES de limpar cookie (se autenticado) para audit (Story 1.8).
        // Idempotente: se sem cookie/expirado, userId fica null e audit é pulado.
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
        // Defesa em profundidade (P5 do code review 2026-05-03): @PreAuthorize
        // explícito redundante com .requestMatchers("/api/v1/**").authenticated()
        // do filter chain — se algum dia alguém regredir a regra do filter,
        // o method-level mantém o endpoint fechado.
        // O auth é injetado pelo Spring (resolve subtipos de Authentication a
        // partir do SecurityContext populado pelo JwtFilter). Se ausente, o
        // SecurityFilterChain já bloqueou em 401 via JwtAuthenticationEntryPoint.
        UserDto dto = authService.buildMeResponse(auth.userId());
        return ResponseEntity.ok(new MeResponse(dto));
        // Nenhum novo Set-Cookie — refresh token está fora de escopo (ADR-0008).
    }
}
