import { api } from "@/services/api";

/**
 * Wrappers do contrato de autenticação backend (Story 1.3 + 1.4).
 *
 * Responsabilidade: traduzir HTTP em Promise; nada de estado React aqui.
 * Quem hidrata user/contexto é o AuthContext (Story 1.5).
 *
 * Convenção: cookie httpOnly AUTH_TOKEN é enviado automaticamente pelo browser
 * via `withCredentials: true` (services/api.js). NUNCA referenciar `document.cookie`
 * nem persistir token em localStorage — vetor XSS direto (R-PRE-01 + S2).
 */

/**
 * Autentica via POST /auth/login.
 *
 * @param {string} email
 * @param {string} password
 * @returns {Promise<{ user: { id, name, profile, linkedCourses } }>}
 * @throws AxiosError em qualquer status != 200. O caller (Login.jsx) inspeciona
 *         `error.response?.data?.code`:
 *           - "AUTH_001" → credenciais inválidas (toast genérico)
 *           - outro / sem response → erro de rede ou 5xx (toast fallback)
 */
export async function login(email, password) {
  const { data } = await api.post("/auth/login", { email, password });
  return data;
}

/**
 * Recupera o usuário a partir do cookie AUTH_TOKEN (boot inicial ou F5).
 *
 * 401 esperado quando cookie ausente / expirado / inválido — o AuthProvider
 * trata silenciosamente (seta user=null) e o RoleGuard decide o redirect.
 *
 * @returns {Promise<{ user: { id, name, profile, linkedCourses } }>}
 */
export async function me() {
  const { data } = await api.get("/auth/me");
  return data;
}

/**
 * Encerra sessão chamando POST /auth/logout (Story 1.6).
 *
 * Erros de rede são engolidos silenciosamente — frontend ainda precisa limpar
 * state local mesmo se backend estiver down (segurança UX: usuário sempre
 * consegue "sair").
 */
export async function logout() {
  try {
    await api.post("/auth/logout");
  } catch {
    // Silencioso por design — backend down não pode bloquear logout local.
  }
}
