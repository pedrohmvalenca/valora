import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useLocation, useNavigate } from "react-router-dom";

import { useAuth } from "@/contexts/AuthContext";
import { landingFor } from "@/lib/landing";
import { Profile } from "@/lib/constants";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";

// Story 1.12 (ADR-0007): mesma chave usada pelo AuthContext no bootstrap
// quando detecta cookie STUDENT no PWA. Lida UMA vez no mount e limpa do storage.
const PWA_STUDENT_BLOCKED_KEY = "valora.pwa_student_blocked";
const PWA_STUDENT_BLOCKED_MSG =
  "Alunos devem usar o aplicativo mobile do VALORA. Esta plataforma é exclusiva para Administrador e Coordenador.";

/**
 * Schema zod inline (Story 1.5 AC5).
 *
 * Mensagens em PT-BR — interface é UX (regra das duas camadas, ADR-0014).
 * `min(6)` espelha o `@Size(min=6)` do `LoginRequest` no backend (Story 1.3);
 * mantido aqui para feedback rápido sem round-trip ao servidor.
 */
const loginSchema = z.object({
  email: z
    .string()
    .min(1, "E-mail é obrigatório")
    .email("E-mail inválido"),
  password: z
    .string()
    .min(6, "Senha deve ter ao menos 6 caracteres")
    .max(100, "Senha muito longa"),
});

export default function Login() {
  const { login, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  // Story 1.12 (ADR-0007): se o bootstrap do AuthContext detectou cookie STUDENT
  // (cenário F5 defensivo), sinalizou via sessionStorage. Lemos UMA vez no init
  // do state e limpamos — banner fica até o usuário fechar.
  const [studentBlockedBanner, setStudentBlockedBanner] = useState(() => {
    if (typeof window === "undefined") return false;
    const flag = sessionStorage.getItem(PWA_STUDENT_BLOCKED_KEY);
    if (flag) sessionStorage.removeItem(PWA_STUDENT_BLOCKED_KEY);
    return flag === "1";
  });

  const form = useForm({
    resolver: zodResolver(loginSchema),
    mode: "onBlur",
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit({ email, password }) {
    try {
      const user = await login(email, password);
      // Story 1.12 (ADR-0007): STUDENT é mobile-only. Backend autenticou e
      // setou o cookie; precisamos limpar via logout antes de devolver para
      // /login (não dá pra deixar o cookie pendurado). Toast persistente — sem
      // navigate, sem state autenticado. Esta checagem precede a 1.11.
      if (user?.profile === Profile.STUDENT) {
        await logout();
        toast.error(PWA_STUDENT_BLOCKED_MSG, { duration: 10000 });
        return;
      }
      // Story 1.11: troca forçada precede o resto do fluxo — vai direto pra
      // /trocar-senha sem bouncing via landing. RoleGuard é safety net.
      if (user?.mustChangePassword) {
        navigate("/trocar-senha", { replace: true });
        return;
      }
      // AC4: respeita state.from (rota tentada antes do login forçado),
      // exceto se for /login (evita loop hipotético).
      // Patch P6 (code review Story 1.5): user?.profile defensivo —
      // AuthContext pode devolver null se backend retornar 200 sem `user`.
      const from = location.state?.from?.pathname;
      const dest = from && from !== "/login" ? from : landingFor(user?.profile);
      navigate(dest, { replace: true });
    } catch (error) {
      const code = error.response?.data?.code;
      const isCredentialsError = code === "AUTH_001";
      // AC6: mensagem genérica anti-enumeração — espelha defesa do backend.
      // AC7: fallback para erro de rede / 5xx.
      toast.error(
        isCredentialsError
          ? "Credenciais inválidas"
          : "Erro inesperado — tente novamente em instantes",
      );
    }
  }

  const { isSubmitting } = form.formState;

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
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>E-mail</FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        placeholder="seu.email@senac.pe"
                        autoComplete="email"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Senha</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="current-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* Patch code-review #5: `disabled={isSubmitting}` apenas — o
                  `mode: "onBlur"` deixava `isValid=false` até o primeiro blur,
                  e usuário que digitava + clicava direto encontrava botão
                  desabilitado. Validação ainda acontece via RHF.handleSubmit
                  (campos inválidos mostram mensagem via FormMessage e o submit
                  não dispara). */}
              <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? "Entrando..." : "Entrar"}
              </Button>

              <p className="text-xs text-muted-foreground text-center">
                Use suas credenciais Senac PE.
              </p>
            </form>
          </Form>
        </CardContent>

        <CardFooter className="text-xs text-muted-foreground justify-center">
          Senac PE — Projeto Integrador ADS-3
        </CardFooter>
      </Card>
    </div>
  );
}
