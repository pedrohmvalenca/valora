import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";

import { useAuth } from "@/contexts/AuthContext";
import { landingFor } from "@/lib/landing";
import * as authService from "@/services/auth";
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
 * Tela de troca forçada — Story 1.11.
 *
 * Renderizada quando `mustChangePassword=true`. RoleGuard impede acesso a
 * qualquer outra rota até a troca ser concluída — esta página é a única saída
 * autenticada (logout também — botão em outra UI; sair > entrar > insistir
 * ainda cai aqui).
 *
 * Dev Notes (spec): senha atual NÃO é pré-preenchida — pré-preencher cria viés
 * de "confirmar sem ler" e degrada o efeito de segurança. Usuário precisa
 * digitar a provisória que recebeu para provar que a teve em mãos.
 *
 * Política da story: mínimo 8 chars; sem complexidade obrigatória.
 */
const schema = z
  .object({
    currentPassword: z.string().min(1, "Senha atual é obrigatória"),
    newPassword: z
      .string()
      .min(8, "Nova senha deve ter ao menos 8 caracteres")
      .max(100, "Nova senha muito longa"),
    confirmPassword: z.string().min(1, "Confirme a nova senha"),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: "As senhas não coincidem",
    path: ["confirmPassword"],
  })
  .refine((data) => data.newPassword !== data.currentPassword, {
    message: "A nova senha não pode ser igual à atual",
    path: ["newPassword"],
  });

export default function TrocarSenha() {
  const { profile, markPasswordChanged } = useAuth();
  const navigate = useNavigate();

  const form = useForm({
    resolver: zodResolver(schema),
    mode: "onBlur",
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  async function onSubmit({ currentPassword, newPassword }) {
    try {
      await authService.changePassword({ currentPassword, newPassword });
      // Sucesso (204): zera flag local e libera o restante do app.
      markPasswordChanged();
      toast.success("Senha alterada com sucesso");
      navigate(landingFor(profile), { replace: true });
    } catch (error) {
      const code = error.response?.data?.code;
      if (code === "AUTH_001") {
        // Senha atual errada — feedback no campo, sem dar dica sobre a regra.
        form.setError("currentPassword", { message: "Senha atual incorreta" });
        toast.error("Senha atual incorreta");
      } else if (code === "AUTH_002") {
        // Sessão expirou no meio do preenchimento — RoleGuard vai pegar no
        // próximo render (user vira null → /login), mas avisamos o usuário pra
        // ele não ficar olhando pro form sem entender.
        toast.error("Sessão expirou — entre novamente para trocar a senha");
        navigate("/login", { replace: true });
      } else if (code === "VAL_001") {
        // Validação backend (defesa em profundidade — zod já deveria ter pego).
        toast.error("Verifique os campos e tente novamente");
      } else {
        toast.error("Erro inesperado — tente novamente em instantes");
      }
    }
  }

  const { isSubmitting } = form.formState;

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md">
        <CardHeader>
          <CardTitle className="text-2xl">Trocar senha</CardTitle>
          <p className="text-sm text-muted-foreground">
            Por segurança, defina uma senha pessoal antes de continuar.
          </p>
        </CardHeader>

        <CardContent>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4" noValidate>
              <FormField
                control={form.control}
                name="currentPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Senha atual (provisória)</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="current-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="newPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Nova senha</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="new-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="confirmPassword"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Confirme a nova senha</FormLabel>
                    <FormControl>
                      <Input type="password" autoComplete="new-password" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Button type="submit" className="w-full" disabled={isSubmitting}>
                {isSubmitting ? "Salvando..." : "Trocar senha"}
              </Button>

              <p className="text-xs text-muted-foreground text-center">
                Mínimo 8 caracteres.
              </p>
            </form>
          </Form>
        </CardContent>

        <CardFooter className="text-xs text-muted-foreground justify-center">
          Senac PE — VALORA
        </CardFooter>
      </Card>
    </div>
  );
}
