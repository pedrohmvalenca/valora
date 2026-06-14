import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useLocation, useNavigate } from "react-router-dom";

import { useAuth } from "@/contexts/AuthContext";
import { landingFor } from "@/lib/landing";
import { Profile } from "@/lib/constants";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

const PWA_STUDENT_BLOCKED_KEY = "valora.pwa_student_blocked";
const PWA_STUDENT_BLOCKED_MSG =
  "Alunos devem usar o aplicativo mobile do VALORA. Esta plataforma é exclusiva para Administrador e Coordenador.";

function validateLogin(email, password) {
  const errors = {};
  if (!email) {
    errors.email = "E-mail é obrigatório";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.email = "E-mail inválido";
  }
  if (!password) {
    errors.password = "Senha é obrigatória";
  } else if (password.length < 6) {
    errors.password = "Senha deve ter ao menos 6 caracteres";
  } else if (password.length > 100) {
    errors.password = "Senha muito longa";
  }
  return errors;
}

export default function Login() {
  const { login, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [studentBlockedBanner, setStudentBlockedBanner] = useState(false);

  useEffect(() => {
    if (typeof window === "undefined") return;
    const flag = sessionStorage.getItem(PWA_STUDENT_BLOCKED_KEY);
    if (flag === "1") {
      setStudentBlockedBanner(true);
      sessionStorage.removeItem(PWA_STUDENT_BLOCKED_KEY);
    }
  }, []);

  async function handleSubmit(event) {
    event.preventDefault();
    const validationErrors = validateLogin(email, password);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setIsSubmitting(true);
    try {
      const user = await login(email, password);
      if (user?.profile === Profile.STUDENT) {
        await logout();
        toast.error(PWA_STUDENT_BLOCKED_MSG, { duration: 10000 });
        return;
      }
      if (user?.mustChangePassword) {
        navigate("/trocar-senha", { replace: true });
        return;
      }
      const from = location.state?.from?.pathname;
      const dest = from && from !== "/login" ? from : landingFor(user?.profile);
      navigate(dest, { replace: true });
    } catch (error) {
      const code = error.response?.data?.code;
      const isCredentialsError = code === "AUTH_001";
      toast.error(
        isCredentialsError
          ? "Credenciais inválidas"
          : "Erro inesperado — tente novamente em instantes",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">VALORA · Login</CardTitle>
        </CardHeader>

        {studentBlockedBanner && (
          <div
            className="mx-6 mb-2 rounded-md border border-destructive/40 bg-destructive/10 px-4 py-3 text-sm text-foreground"
            role="alert"
            aria-live="polite"
          >
            <div className="flex items-start justify-between gap-3">
              <p className="flex-1">{PWA_STUDENT_BLOCKED_MSG}</p>
              <button
                type="button"
                onClick={() => setStudentBlockedBanner(false)}
                className="text-xs text-muted-foreground hover:text-foreground underline shrink-0"
                aria-label="Fechar aviso"
              >
                OK
              </button>
            </div>
          </div>
        )}

        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm font-medium">E-mail</label>
              <Input
                id="email"
                type="email"
                placeholder="seu.email@senac.pe"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
              {errors.email && (
                <p className="text-sm text-destructive">{errors.email}</p>
              )}
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="text-sm font-medium">Senha</label>
              <Input
                id="password"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              {errors.password && (
                <p className="text-sm text-destructive">{errors.password}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Entrando..." : "Entrar"}
            </Button>

            <p className="text-xs text-muted-foreground text-center">
              Use suas credenciais Senac PE.
            </p>
          </form>
        </CardContent>

        <CardFooter className="text-xs text-muted-foreground justify-center">
          Senac PE — Projeto Integrador ADS-3
        </CardFooter>
      </Card>
    </div>
  );
}
