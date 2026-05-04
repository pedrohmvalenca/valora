package br.com.senac.valora.controllers;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Base abstrata de Controllers protegidos — DEFAULT DENY no nível da classe.
 *
 * <p>Toda Controller que estende esta classe herda {@code @PreAuthorize("denyAll()")}
 * de classe; cada método que precisa de acesso deve declarar seu próprio
 * {@code @PreAuthorize("hasRole(...)")} explicitamente.
 *
 * <p>Esquecer a anotação no método = 403, NÃO 200. Isso operacionaliza a
 * RNF-0004 (RBAC em todas as camadas) e fecha a janela de "endpoint sem
 * autorização explícita por engano".
 *
 * <p>Coexiste com o {@code .anyRequest().denyAll()} do {@code SecurityConfig}:
 * o filter chain garante DEFAULT DENY no nível de URL; este base controller
 * garante no nível de method invocation (defesa em profundidade).
 */
@PreAuthorize("denyAll()")
public abstract class BaseSecuredController {
}
