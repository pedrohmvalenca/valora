package br.com.senac.valora.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuração de segurança em method level — Spring Security 6.
 *
 * <p>{@link EnableMethodSecurity}{@code (prePostEnabled = true)} substitui o
 * deprecado {@code @EnableGlobalMethodSecurity}. Ativa o suporte a
 * {@code @PreAuthorize} nos beans Spring (incluindo o
 * {@link br.com.senac.valora.controllers.BaseSecuredController}).
 *
 * <p>Define o {@link RoleHierarchy} e o
 * {@link DefaultMethodSecurityExpressionHandler} aqui — fonte única para
 * expansão de roles tanto no filter chain (request matchers) quanto em
 * checagens de method (@{@code PreAuthorize}). Em Spring Security 6 o
 * expression handler precisa ser registrado <b>explicitamente</b> com o
 * {@code RoleHierarchy} para que {@code hasRole(...)} use a hierarquia em
 * method level (o filter chain herda automaticamente).
 *
 * <p>Hierarquia (ADR-0011 + project-context §5.2):
 * <pre>
 *   ROLE_ADMINISTRATOR &gt; ROLE_COORDINATOR
 *   ROLE_COORDINATOR &gt; ROLE_STUDENT
 * </pre>
 *
 * <p>O {@link RoleHierarchy} bean expande as authorities automaticamente.
 * O {@link JwtAuthentication} retorna apenas <b>uma</b> authority por profile
 * (ROLE_ADMINISTRATOR, ROLE_COORDINATOR ou ROLE_STUDENT) — Spring expande
 * durante o check.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig {

    @Bean
    public RoleHierarchy roleHierarchy() {
        // RoleHierarchyImpl.fromHierarchy(...) é o factory recomendado em
        // Spring Security 6.3+ (substitui setHierarchy deprecated).
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMINISTRATOR > ROLE_COORDINATOR
                ROLE_COORDINATOR > ROLE_STUDENT
                """);
    }

    /**
     * Expression handler customizado para que {@code hasRole(...)} em
     * {@code @PreAuthorize} use a hierarquia de roles. Sem este bean,
     * apenas o filter chain enxergaria a hierarquia — methods continuariam
     * exigindo a authority exata.
     */
    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
