package br.com.senac.valora.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

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

    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }
}
