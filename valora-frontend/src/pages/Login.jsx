import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useLocation, useNavigate } from "react-router-dom";

import { useAuth } from "@/contexts/AuthContext";
import { landingFor } from "@/lib/landing";
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
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const form = useForm({
    resolver: zodResolver(loginSchema),
    mode: "onBlur",
    defaultValues: { email: "", password: "" },
  });

  async function onSubmit({ email, password }) {
    try {
      const user = await login(email, password);
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
