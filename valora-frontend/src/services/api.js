import axios from "axios";

/**
 * Instância axios única do VALORA (arch § F2).
 *
 * Pontos críticos:
 * - withCredentials: true → o browser envia o cookie httpOnly AUTH_TOKEN
 *   automaticamente em toda chamada. SEM isso, /auth/me sempre retorna 401
 *   mesmo com cookie válido.
 * - baseURL via env: dev local aponta para Spring Boot em :8080;
 *   Vercel prod aponta para Render via VITE_API_BASE.
 * - Timeout 15s: limiar percebido pela UX antes de feedback negativo;
 *   cold start raro ultrapassa esse limite no Render free tier.
 */
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || "/api/v1",
  withCredentials: true,
  timeout: 15000,
});

/**
 * Interceptor 401: redireciona para /login EXCETO quando a chamada é
 * /auth/login (deixa o form tratar via toast — AC6) ou /auth/me (deixa o
 * AuthProvider decidir no boot — AC2).
 *
 * Hard reload via window.location.href é proposital: limpa qualquer estado
 * React stale após sessão expirar e força re-bootstrap completo (boot via
 * /auth/me que retornará 401 → user=null → guarda manda para login).
 *
 * useNavigate() NÃO funciona aqui — interceptor roda fora do React tree.
 */
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const url = error.config?.url ?? "";
    const status = error.response?.status;
    const isAuthEndpoint = url.includes("/auth/login") || url.includes("/auth/me");
    // Patch P1 (code review Story 1.5): não redirecionar se já estamos em /login —
    // evita reload visual em cima de /login e perda dos valores do form quando uma
    // chamada paralela (ex.: /auth/logout futuro) retornar 401.
    const alreadyOnLogin = window.location.pathname === "/login";
    if (status === 401 && !isAuthEndpoint && !alreadyOnLogin) {
      window.location.href = "/login";
    }
    return Promise.reject(error);
  },
);
