import { useEffect } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { toast } from "sonner";

import { useAuth } from "@/contexts/AuthContext";
import { landingFor } from "@/lib/landing";

/**
 * Guarda de rota — Story 1.5.
 *
 * Responsabilidades (em ordem de avaliação):
 *  1. Espera o boot do AuthContext (`isLoading`) para não piscar /login no F5.
 *  2. Sem usuário autenticado → manda para /login preservando `state.from`,
 *     para que o <Login> retome o destino tentado após autenticar (AC4).
 *  3. Com `roles` declarado e perfil fora da whitelist → toast genérico em
 *     PT-BR + redirect para landing do perfil atual (AC3).
 *  4. Caso contrário, libera o conteúdo.
 *
 * Sem prop `roles` → comportamento "exige autenticação" apenas (compatível com
 * o `<ProtectedShell>` da Story 1.2 — Admin/Coord/Student passam pelo AppShell;
 * filtragem por perfil de items é responsabilidade do AppShellSidebar).
 *
 * Stories 2.x+ envolvem rotas restritas com `<RoleGuard roles={['ADMINISTRATOR']}>`.
 */
export function RoleGuard({ roles, children }) {
  const { isAuthenticated, isBootstrapping, profile, mustChangePassword } = useAuth();
  const location = useLocation();

  // Patch P4 (code review Story 1.5): se `roles` está declarado, profile=null
  // deve negar (não passar). Versão anterior `roles && profile && !roles.includes(profile)`
  // retornava false quando profile=null, deixando user passar pelo guard.
  const isProfileMismatch = Boolean(roles && (!profile || !roles.includes(profile)));

  // Toast em useEffect (não em render) — evita warnings React de side effect
  // e múltiplos toasts em re-renders.
  useEffect(() => {
    if (isProfileMismatch) {
      toast.error("Acesso não permitido para seu perfil");
    }
  }, [isProfileMismatch]);

  // 1) Boot em andamento — null evita pisca para /login no F5
  // Patch code-review #3: usa `isBootstrapping` (não `isLoading`) para que
  // login() em andamento não desmonte rotas autenticadas em re-renders.
  if (isBootstrapping) return null;

  // 2) Não autenticado — manda para login preservando destino tentado
  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 3) Story 1.11 — troca de senha forçada. Bloqueia QUALQUER outra rota até a
  // flag zerar (precede o profile check). Exceção: a própria /trocar-senha
  // (senão loop infinito). Defesa em profundidade: usuário não consegue pular
  // a tela alterando URL no navegador.
  // Patch defer code review 2026-06-09 (D4): normaliza pathname antes do match
  // — `/trocar-senha/` ou `/Trocar-Senha` agora também batem na exceção e não
  // entram em redirect loop.
  if (
    mustChangePassword &&
    location.pathname.replace(/\/$/, "").toLowerCase() !== "/trocar-senha"
  ) {
    return <Navigate to="/trocar-senha" replace />;
  }

  // 4) Perfil errado — manda para landing do perfil atual
  if (isProfileMismatch) {
    return <Navigate to={landingFor(profile)} replace />;
  }

  // 5) Autorizado
  return children;
}
