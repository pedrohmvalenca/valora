package br.com.senac.valora.services;

import br.com.senac.valora.dtos.UserDto;
import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.mappers.UserMapper;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada de serviço de autenticação.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>{@link #authenticate(String, String)} — valida credenciais; retorna
 *       {@link LoginSuccess} no caminho ok ou lança
 *       {@link org.springframework.security.authentication.BadCredentialsException}
 *       (mensagem genérica anti-enumeração — OWASP A07:2021) que o
 *       {@code GlobalExceptionHandler.handleBadCredentials} converte em 401
 *       com {@code code=AUTH_001}.</li>
 *   <li>{@link #buildMeResponse(UUID)} — recompõe o {@link UserDto} para
 *       {@code GET /auth/me} a partir do userId vindo do {@code SecurityContext}.</li>
 *   <li>{@link #loadUserByUsername(String)} — implementa {@link UserDetailsService}
 *       para integração futura com Spring Security padrão (não usado pelos
 *       endpoints desta story, mas exigido pelo contrato).</li>
 * </ul>
 */
@Service
public class AuthService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Hash BCrypt fixo (cost 12) usado para "queimar" tempo de CPU quando o
     * email não existe — evita timing oracle revelando se o email é válido.
     *
     * <p>Hash gerado em 2026-05-03 a partir de uma string aleatória qualquer;
     * o valor concreto não importa, só o custo computacional.
     */
    private static final String DUMMY_HASH =
            "$2a$12$M7Z7eqHWnGvHkFyxKQbHJOfhmIwfrInwnEbeb5Qb6Lqi6iLP2/V/y";

    private final UserRepository userRepository;
    private final CoordinatorCourseRepository coordinatorCourseRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthService(
            UserRepository userRepository,
            CoordinatorCourseRepository coordinatorCourseRepository,
            BCryptPasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.coordinatorCourseRepository = coordinatorCourseRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = normalizeEmail(email);
        return userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + normalizedEmail));
    }

    /**
     * Autentica usuário por email + senha. Sempre roda BCrypt (mesmo quando
     * email não existe) para evitar timing attack que revele a existência do
     * email. Lança {@link BadCredentialsException} (genérica — anti-enumeração
     * OWASP A07) em qualquer falha; o {@code GlobalExceptionHandler} converte
     * em 401 com {@code code=AUTH_001} e {@code message="Credenciais inválidas"}.
     *
     * @throws BadCredentialsException se email não existe, senha errada ou usuário inativo
     */
    @Transactional(readOnly = true)
    public LoginSuccess authenticate(String email, String password) {
        // Normaliza email (trim + lowercase) — emails são case-insensitive na
        // prática (RFC 5321 recomenda no domain part). Stories 2.6/3.2 que
        // criam User devem aplicar a mesma normalização ao persistir.
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
            // Roda BCrypt mesmo sem usuário para igualar o tempo de resposta
            // ao caminho de senha errada — defesa anti-enumeração.
            passwordEncoder.matches(password, DUMMY_HASH);
            log.warn("Login falhou: email={}", normalizedEmail);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            log.warn("Login falhou: email={}", normalizedEmail);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        if (!user.isActive()) {
            log.warn("Login bloqueado (usuário inativo): email={}", normalizedEmail);
            throw new BadCredentialsException("Credenciais inválidas");
        }

        List<UUID> linkedCourses = resolveLinkedCourses(user);
        String token = jwtService.generateToken(user.getId(), user.getProfile());
        return new LoginSuccess(token, user, linkedCourses);
    }

    /**
     * Recompõe o {@link UserDto} para {@code GET /auth/me}. {@code linkedCourses}
     * é re-buscado do banco a cada chamada (não cacheado no JWT) — política
     * intencional para refletir mudanças de vínculo sem precisar reemitir token.
     */
    @Transactional(readOnly = true)
    public UserDto buildMeResponse(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário do JWT não encontrado: id=" + userId));
        List<UUID> linkedCourses = resolveLinkedCourses(user);
        return userMapper.toDto(user, linkedCourses);
    }

    /**
     * Resolve os cursos vinculados conforme o perfil:
     * <ul>
     *   <li>{@code COORDINATOR}: lookup em {@code coordinator_course}.</li>
     *   <li>{@code ADMINISTRATOR}: lista vazia (admin opera em todos os cursos
     *       via {@code <CursoSelector>} — Story 1.7).</li>
     *   <li>{@code STUDENT}: lista vazia em V1 (tabela {@code student_course}
     *       é criada pela Story 3.1).</li>
     * </ul>
     */
    private List<UUID> resolveLinkedCourses(User user) {
        if (user.getProfile() == UserProfile.COORDINATOR) {
            return coordinatorCourseRepository.findCourseIdsByCoordinatorId(user.getId());
        }
        return List.of();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    // ============================================================
    // Tipo de retorno do authenticate()
    // ============================================================

    /**
     * Resultado bem-sucedido da autenticação. Falhas são sinalizadas via
     * {@link BadCredentialsException} (mapeada pelo {@code GlobalExceptionHandler}
     * para 401 + {@code code=AUTH_001}); por isso este record só carrega o caminho ok.
     */
    public record LoginSuccess(String token, User user, List<UUID> linkedCourses) {}
}
