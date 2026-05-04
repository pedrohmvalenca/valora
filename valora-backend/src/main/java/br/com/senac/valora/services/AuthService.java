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

@Service
public class AuthService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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

    @Transactional(readOnly = true)
    public LoginSuccess authenticate(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);

        if (userOpt.isEmpty()) {
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

    @Transactional(readOnly = true)
    public UserDto buildMeResponse(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Usuário do JWT não encontrado: id=" + userId));
        List<UUID> linkedCourses = resolveLinkedCourses(user);
        return userMapper.toDto(user, linkedCourses);
    }

    private List<UUID> resolveLinkedCourses(User user) {
        if (user.getProfile() == UserProfile.COORDINATOR) {
            return coordinatorCourseRepository.findCourseIdsByCoordinatorId(user.getId());
        }
        return List.of();
    }

    private static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public record LoginSuccess(String token, User user, List<UUID> linkedCourses) {}
}
