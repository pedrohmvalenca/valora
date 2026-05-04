package br.com.senac.valora.config;

import br.com.senac.valora.security.JwtAccessDeniedHandler;
import br.com.senac.valora.security.JwtAuthenticationEntryPoint;
import br.com.senac.valora.security.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de Spring Security do VALORA — DEFAULT DENY (RNF-0004).
 *
 * <p>Decisões fixadas:
 * <ul>
 *   <li>{@code csrf().disable()}: JWT em cookie httpOnly + {@code SameSite=Strict}
 *       já mitiga CSRF; sem sessão server-side, não há outro state a proteger.</li>
 *   <li>{@code SessionCreationPolicy.STATELESS}: não cria HttpSession, evita
 *       leak de estado entre requests.</li>
 *   <li>{@link BCryptPasswordEncoder} cost 12 (RNF-0003 / Architecture S3).</li>
 *   <li>{@link JwtFilter} <b>antes</b> do {@code UsernamePasswordAuthenticationFilter}
 *       para popular {@code SecurityContext} a partir do cookie.</li>
 *   <li><b>DEFAULT DENY</b> (Story 1.4): {@code .anyRequest().denyAll()} fecha
 *       qualquer rota não explicitamente listada. Controllers usam
 *       {@code @PreAuthorize} via {@code BaseSecuredController} para abrir.</li>
 *   <li>Actuator: {@code /health} público; demais endpoints só ADMINISTRATOR.</li>
 *   <li>{@link JwtAuthenticationEntryPoint} e {@link JwtAccessDeniedHandler}
 *       garantem que respostas 401/403 do filter chain sigam o shape JSON
 *       canônico do {@code ErrorResponse}.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt cost 12 — Architecture S3 + RNF-0003.
     * Custo mais baixo que 12 deixa hash quebrável em GPU; mais alto degrada login.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtFilter jwtFilter,
            JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
            JwtAccessDeniedHandler jwtAccessDeniedHandler)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // Customizer.withDefaults() faz Spring Security usar o bean nomeado
                // 'corsConfigurationSource' (definido em CorsConfig) sem precisar
                // de injeção explícita — evita colisão com mvcHandlerMappingIntrospector,
                // que também implementa CorsConfigurationSource em Spring Security 6.
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> req
                        // Allow-list explícita: rotas que devem ficar abertas
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        // Actuator (exceto /health) só ADMINISTRATOR — resolve deferred-work Story 1.1
                        .requestMatchers("/actuator/**").hasRole("ADMINISTRATOR")
                        // Endpoints da API exigem autenticação no filter chain;
                        // a autorização fina por papel é responsabilidade do method-level
                        // @PreAuthorize (via BaseSecuredController + RoleHierarchy).
                        .requestMatchers("/api/v1/**").authenticated()
                        // DEFAULT DENY (RNF-0004): qualquer rota fora dos blocos acima
                        // (paths não-mapeados, recursos estáticos inexistentes, etc.)
                        // cai em denyAll() — fecha a janela de "abriu sem querer".
                        .anyRequest().denyAll())
                // Entry point + access denied handler customizados respondem com
                // ErrorResponse JSON (mesmo shape do GlobalExceptionHandler).
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
