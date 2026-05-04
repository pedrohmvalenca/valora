/**
 * Mapeia códigos do backend (em inglês) para labels exibidos ao usuário (PT-BR).
 *
 * Convenção do projeto (regra das duas camadas — ver `CLAUDE.md` § 3 e ADR-0014):
 * código em inglês casa com o JSON do backend; UI exibe em PT-BR.
 *
 * Uso típico:
 *   import { PROFILE_LABELS } from "@/lib/i18n";
 *   const { user, profile } = useAuth();
 *   return <span>Bem-vindo, {user?.name} ({PROFILE_LABELS[profile]})</span>;
 */
import { Profile } from "@/lib/constants";

export const PROFILE_LABELS = Object.freeze({
  [Profile.ADMINISTRATOR]: "Administrador",
  [Profile.COORDINATOR]: "Coordenador",
  [Profile.STUDENT]: "Aluno",
});
