package br.com.senac.valora.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.senac.valora.entities.User;
import br.com.senac.valora.entities.UserProfile;
import br.com.senac.valora.mappers.UserMapper;
import br.com.senac.valora.repositories.CoordinatorCourseRepository;
import br.com.senac.valora.repositories.UserRepository;
import br.com.senac.valora.services.AuthService.LoginSuccess;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Testes unitários de {@link AuthService}.
 *
 * <p>Cobertura:
 * <ul>
 *   <li>Login OK para Admin (linkedCourses vazio)</li>
 *   <li>Login OK para Coordinator (linkedCourses populado via repository)</li>
 *   <li>Login OK para Student (linkedCourses vazio)</li>
 *   <li>Email inexistente → INVALID + BCrypt rodado contra DUMMY_HASH (anti-timing)</li>
 *   <li>Senha errada → INVALID</li>
 *   <li>Usuário inativo → INVALID</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CoordinatorCourseRepository coordinatorCourseRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;

    @InjectMocks private AuthService authService;

    private User adminUser;
    private User coordUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(UUID.randomUUID())
                .email("admin@valora.local")
                .passwordHash("$2a$12$hash-admin")
                .name("Admin Test")
                .profile(UserProfile.ADMINISTRATOR)
                .isActive(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        coordUser = User.builder()
                .id(UUID.randomUUID())
                .email("coord@valora.local")
                .passwordHash("$2a$12$hash-coord")
                .name("Coord Test")
                .profile(UserProfile.COORDINATOR)
                .isActive(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
        studentUser = User.builder()
                .id(UUID.randomUUID())
                .email("aluno@valora.local")
                .passwordHash("$2a$12$hash-aluno")
                .name("Aluno Test")
                .profile(UserProfile.STUDENT)
                .isActive(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    @Test
    void authenticate_admin_retornaSuccessComLinkedCoursesVazio() {
        when(userRepository.findByEmail("admin@valora.local")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", adminUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(adminUser.getId(), UserProfile.ADMINISTRATOR))
                .thenReturn("fake-jwt-admin");

        LoginSuccess result = authService.authenticate("admin@valora.local", "Admin@123");

        assertThat(result.token()).isEqualTo("fake-jwt-admin");
        assertThat(result.user()).isSameAs(adminUser);
        assertThat(result.linkedCourses()).isEmpty();
        // Admin não consulta junction
        verify(coordinatorCourseRepository, never()).findCourseIdsByCoordinatorId(any());
    }

    @Test
    void authenticate_coordinator_retornaSuccessComLinkedCoursesDoRepository() {
        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        when(userRepository.findByEmail("coord@valora.local")).thenReturn(Optional.of(coordUser));
        when(passwordEncoder.matches("Coord@123", coordUser.getPasswordHash())).thenReturn(true);
        when(coordinatorCourseRepository.findCourseIdsByCoordinatorId(coordUser.getId()))
                .thenReturn(List.of(c1, c2));
        when(jwtService.generateToken(coordUser.getId(), UserProfile.COORDINATOR))
                .thenReturn("fake-jwt-coord");

        LoginSuccess result = authService.authenticate("coord@valora.local", "Coord@123");

        assertThat(result.linkedCourses()).containsExactly(c1, c2);
    }

    @Test
    void authenticate_student_retornaSuccessComLinkedCoursesVazioEmV1() {
        when(userRepository.findByEmail("aluno@valora.local")).thenReturn(Optional.of(studentUser));
        when(passwordEncoder.matches("Aluno@123", studentUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(studentUser.getId(), UserProfile.STUDENT))
                .thenReturn("fake-jwt-student");

        LoginSuccess result = authService.authenticate("aluno@valora.local", "Aluno@123");

        assertThat(result.linkedCourses()).isEmpty();
        // Student-course junction não existe em V1
        verify(coordinatorCourseRepository, never()).findCourseIdsByCoordinatorId(any());
    }

    @Test
    void authenticate_emailInexistente_lancaBadCredentialsERodaBCryptContraDummy() {
        when(userRepository.findByEmail("nao-existe@x.y")).thenReturn(Optional.empty());

        // Mensagem genérica anti-enumeração — handler global converte em AUTH_001
        assertThatThrownBy(() -> authService.authenticate("nao-existe@x.y", "qualquer"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");

        // Crítico: anti-timing — BCrypt SEMPRE roda mesmo sem usuário
        verify(passwordEncoder, times(1)).matches(eq("qualquer"), anyString());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void authenticate_senhaErrada_lancaBadCredentials() {
        when(userRepository.findByEmail("admin@valora.local")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("senha-errada", adminUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate("admin@valora.local", "senha-errada"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");

        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void authenticate_usuarioInativo_lancaBadCredentials() {
        adminUser.setActive(false);
        when(userRepository.findByEmail("admin@valora.local")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("Admin@123", adminUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate("admin@valora.local", "Admin@123"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Credenciais inválidas");

        verify(jwtService, never()).generateToken(any(), any());
    }
}
