/**
 * Enum de perfis VALORA — alinhado com CHECK constraint de `users.profile`
 * em V1__init_identity.sql (Story 1.1) e com `UserProfile` enum do backend
 * (ADR-0013/0014: naming inglês no código; PT-BR só na UX).
 *
 * Para exibição em UI ("Administrador", "Coordenador", "Aluno") usar
 * `PROFILE_LABELS` em `src/lib/i18n.js`.
 *
 * Story 1.5 hidrata `linkedCourses` via GET /api/v1/auth/me.
 * Story 1.7 implementa CourseContext com persistência URL+localStorage.
 */
export const Profile = Object.freeze({
  ADMINISTRATOR: "ADMINISTRATOR",
  COORDINATOR: "COORDINATOR",
  STUDENT: "STUDENT",
});
