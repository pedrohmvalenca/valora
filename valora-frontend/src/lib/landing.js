import { Profile } from "@/lib/constants";

/**
 * Landing per profile — fonte única consumida por <Login>, <IndexRedirect>,
 * <LoginRoute> e <RoleGuard>.
 *
 * Decisão (Story 1.5): rotas FLAT (sem prefixo /admin /coord /aluno) — alinhado
 * com AppShellSidebar.jsx da Story 1.2. Migrar para prefixadas requereria
 * recriar todo o sidebar; não é o trade-off certo agora. Se o time decidir
 * mudar, o trabalho fica isolado neste arquivo + App.jsx.
 *
 * Profile desconhecido cai em "/" (Admin Dashboard placeholder) — defesa em
 * profundidade contra regressão de contrato (backend devolvendo enum novo
 * não previsto). Patch P2 (code review Story 1.5): default "/login" causava
 * loop infinito quando consumido por <LoginRoute> (autenticado → Navigate
 * para landingFor → /login → re-render → loop).
 */
export function landingFor(profile) {
  switch (profile) {
    case Profile.ADMINISTRATOR:
      return "/";
    case Profile.COORDINATOR:
      return "/submissoes";
    case Profile.STUDENT:
      return "/minhas-submissoes";
    default:
      return "/";
  }
}
