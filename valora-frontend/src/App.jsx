import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { Toaster } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { ThemeProvider } from "@/contexts/ThemeProvider";
import { AuthProvider, useAuth } from "@/contexts/AuthContext";
import { CursoProvider } from "@/contexts/CursoContext";
import { RoleGuard } from "@/components/RoleGuard";
import { AppShell } from "@/components/AppShell";
import { Profile } from "@/lib/constants";
import { landingFor } from "@/lib/landing";
import Login from "@/pages/Login";
import Index from "@/pages/Index";
import NotFound from "@/pages/NotFound";
import Submissoes from "@/pages/Submissoes";
import Cursos from "@/pages/Cursos";
import Coordenadores from "@/pages/Coordenadores";
import Alunos from "@/pages/Alunos";
import Categorias from "@/pages/Categorias";
import Pendencias from "@/pages/Pendencias";
import MinhasSubmissoes from "@/pages/MinhasSubmissoes";
import Logs from "@/pages/Logs";

function ProtectedShell() {
  return (
    <RoleGuard>
      <AppShell>
        <AppShell.Sidebar />
        <AppShell.Main>
          <AppShell.Header />
          <AppShell.Content />
        </AppShell.Main>
      </AppShell>
    </RoleGuard>
  );
}

/**
 * Rota "/" diferenciada por perfil — Story 1.5 AC1/AC9.
 *
 * Admin tem dashboard real (placeholder atual; Story 5.4 popula KPIs); demais
 * perfis são redirecionados para a landing canônica via `landingFor()`.
 * Renderiza null durante boot para evitar flicker.
 */
function IndexRedirect() {
  const { profile, isBootstrapping } = useAuth();
  if (isBootstrapping) return null;
  if (profile === Profile.ADMINISTRATOR) return <Index />;
  return <Navigate to={landingFor(profile)} replace />;
}

/**
 * /login — quando o usuário já tem sessão, vai direto para a landing do
 * perfil em vez de "/" fixo (evita pisca extra em refresh com cookie válido).
 *
 * Patch code-review #3: usa `isBootstrapping` (não `isLoading`) — caso contrário
 * o `setIsLoading(true)` durante `login()` desmontaria o <Login> e os campos
 * perderiam valor em cenário de erro (credencial inválida).
 */
function LoginRoute() {
  const { isAuthenticated, isBootstrapping, profile } = useAuth();
  if (isBootstrapping) return null;
  if (isAuthenticated) return <Navigate to={landingFor(profile)} replace />;
  return <Login />;
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginRoute />} />
      <Route element={<ProtectedShell />}>
        <Route path="/" element={<IndexRedirect />} />
        {/* Story 4.5 (consolidada γ) — página real */}
        <Route
          path="/submissoes"
          element={
            <RoleGuard roles={[Profile.COORDINATOR, Profile.ADMINISTRATOR]}>
              <Submissoes />
            </RoleGuard>
          }
        />
        {/* Story 1.10 — stub pages com RoleGuard ativo */}
        <Route
          path="/cursos"
          element={
            <RoleGuard roles={[Profile.ADMINISTRATOR]}>
              <Cursos />
            </RoleGuard>
          }
        />
        <Route
          path="/coordenadores"
          element={
            <RoleGuard roles={[Profile.ADMINISTRATOR]}>
              <Coordenadores />
            </RoleGuard>
          }
        />
        <Route
          path="/alunos"
          element={
            <RoleGuard roles={[Profile.COORDINATOR, Profile.ADMINISTRATOR]}>
              <Alunos />
            </RoleGuard>
          }
        />
        <Route
          path="/categorias"
          element={
            <RoleGuard roles={[Profile.COORDINATOR, Profile.ADMINISTRATOR]}>
              <Categorias />
            </RoleGuard>
          }
        />
        <Route
          path="/pendencias"
          element={
            <RoleGuard roles={[Profile.COORDINATOR, Profile.ADMINISTRATOR]}>
              <Pendencias />
            </RoleGuard>
          }
        />
        <Route
          path="/minhas-submissoes"
          element={
            <RoleGuard roles={[Profile.ADMINISTRATOR, Profile.STUDENT]}>
              <MinhasSubmissoes />
            </RoleGuard>
          }
        />
        <Route
          path="/logs"
          element={
            <RoleGuard roles={[Profile.ADMINISTRATOR]}>
              <Logs />
            </RoleGuard>
          }
        />
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default function App() {
  return (
    <ThemeProvider>
      <BrowserRouter>
        <AuthProvider>
          <CursoProvider>
            <TooltipProvider>
              <Toaster />
              <AppRoutes />
            </TooltipProvider>
          </CursoProvider>
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
}
