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
    // Story 1.12 (ADR-0007): STUDENT é mobile-only. PWA bloqueia esse perfil já no
    // login (Login.jsx + AuthContext bootstrap), então este switch nunca recebe
    // STUDENT em runtime. O default "/" abaixo é defesa em profundidade — se algum
    // dia o bloqueio falhar, o usuário cai no Dashboard (que tem RoleGuard,
    // que rebate pra /login).
    default:
      return "/";
  }
}
