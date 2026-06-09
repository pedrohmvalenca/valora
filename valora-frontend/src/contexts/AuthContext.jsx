import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as authService from "@/services/auth";
import { Profile } from "@/lib/constants";

// Story 1.12 (ADR-0007): chave no sessionStorage que sinaliza pro Login.jsx que
// o bootstrap detectou cookie STUDENT no PWA — usado pra renderizar o banner
// persistente em vez de toast (UX mais amistosa que toast em F5).
const PWA_STUDENT_BLOCKED_KEY = "valora.pwa_student_blocked";

/**
 * AuthContext real — Story 1.5.
 *
 * Shape do user (alinhado ao JSON do backend pós-convergência ADR-0013/0014):
 *   { id, name, profile: 'ADMINISTRATOR'|'COORDINATOR'|'STUDENT', linkedCourses: [<UUID>] }
 *
 * Cookie httpOnly AUTH_TOKEN é o storage; React mantém só o objeto user em
 * memória. NUNCA persistir user em localStorage/sessionStorage (RNF-0003 + S2 +
 * R-PRE-01). withCredentials: true (services/api.js) faz o browser enviar o
 * cookie em toda chamada — useAuth nunca toca em document.cookie.
 *
 * Bootstrap: no mount inicial, chama GET /auth/me para hidratar user a partir
 * do cookie (ex.: F5 com sessão ainda válida). Durante a chamada inicial,
 * `isBootstrapping=true` para que RoleGuard/LoginRoute/IndexRedirect renderizem
 * null e evitem pisca para /login.
 *
 * Patch code-review #3 (2026-05-04): `isBootstrapping` é separado de `isLoading`
 * propositadamente. `isLoading` cobre QUALQUER operação async (boot OU login);
 * mas guards de rota usam APENAS `isBootstrapping` — caso contrário, durante o
 * login() o LoginRoute desmonta o <Login> e os campos perdem valor em cenários
 * de erro (credencial inválida etc.). Estado durante login fica controlado por
 * `RHF.isSubmitting` no próprio Login.jsx.
 */
const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isLoading, setIsLoading] = useState(true);

  // Bootstrap silencioso via /auth/me — roda uma única vez no mount
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await authService.me();
        if (cancelled) return;
        const fetched = data?.user ?? null;
        // Story 1.12 (ADR-0007): cenário defensivo — cookie STUDENT chegou no PWA
        // (improvável; mobile e PWA compartilham backend, mas alguém poderia
        // ter setado manualmente ou clonado sessão do mobile). Tratamos como
        // não-autenticado: logout silencioso + sinaliza /login pra mostrar banner.
        if (fetched?.profile === Profile.STUDENT) {
          try {
            await authService.logout();
          } catch {
            // Backend offline não pode bloquear bootstrap; cookie ficará pendurado
            // mas o user em memória já é null, então RoleGuard manda pro login.
          }
          if (typeof window !== "undefined") {
            sessionStorage.setItem(PWA_STUDENT_BLOCKED_KEY, "1");
          }
          if (!cancelled) setUser(null);
        } else if (!cancelled) {
          setUser(fetched);
        }
      } catch {
        // 401 esperado quando cookie ausente/expirado/inválido — silencioso
        if (!cancelled) setUser(null);
      } finally {
        if (!cancelled) {
          setIsBootstrapping(false);
          setIsLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email, password) => {
    setIsLoading(true);
    try {
      const data = await authService.login(email, password);
      setUser(data?.user ?? null);
      return data?.user ?? null;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(async () => {
    try {
      await authService.logout();
    } finally {
      setUser(null);
    }
  }, []);

  // Story 1.11: chamado pela página /trocar-senha após o POST /auth/change-password
  // retornar 204 — zera a flag em memória sem precisar de round-trip extra em /auth/me.
  // Backend já persistiu users.must_change_password=false; o próximo bootstrap puxa
  // o estado correto. Update funcional para não tropeçar em re-renders concorrentes.
  const markPasswordChanged = useCallback(() => {
    setUser((u) => (u ? { ...u, mustChangePassword: false } : u));
  }, []);

  const value = useMemo(
    () => ({
      user,
      profile: user?.profile ?? null,
      isAuthenticated: user !== null,
      // Story 1.11: flag direta para RoleGuard sem precisar destrinchar user
      mustChangePassword: Boolean(user?.mustChangePassword),
      isBootstrapping,
      isLoading,
      login,
      logout,
      markPasswordChanged,
    }),
    [user, isBootstrapping, isLoading, login, logout, markPasswordChanged],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (ctx === null) {
    throw new Error("useAuth precisa ser chamado dentro de <AuthProvider>");
  }
  return ctx;
}
