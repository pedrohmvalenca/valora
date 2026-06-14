import { useState } from "react";
import { toast } from "sonner";
import { useNavigate } from "react-router-dom";

import { useAuth } from "@/contexts/AuthContext";
import { landingFor } from "@/lib/landing";
import * as authService from "@/services/auth";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardFooter, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";

function validatePasswords(currentPassword, newPassword, confirmPassword) {
  const errors = {};
  if (!currentPassword) {
    errors.currentPassword = "Senha atual é obrigatória";
  }
  if (!newPassword) {
    errors.newPassword = "Nova senha é obrigatória";
  } else if (newPassword.length < 8) {
    errors.newPassword = "Nova senha deve ter ao menos 8 caracteres";
  } else if (newPassword.length > 100) {
    errors.newPassword = "Nova senha muito longa";
  } else if (currentPassword && newPassword === currentPassword) {
    errors.newPassword = "A nova senha não pode ser igual à atual";
  }
  if (!confirmPassword) {
    errors.confirmPassword = "Confirme a nova senha";
  } else if (newPassword && confirmPassword !== newPassword) {
    errors.confirmPassword = "As senhas não coincidem";
  }
  return errors;
}

export default function TrocarSenha() {
  const { profile, markPasswordChanged } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    const validationErrors = validatePasswords(currentPassword, newPassword, confirmPassword);
    setErrors(validationErrors);
    if (Object.keys(validationErrors).length > 0) return;

    setIsSubmitting(true);
    try {
      await authService.changePassword({ currentPassword, newPassword });
      markPasswordChanged();
      toast.success("Senha alterada com sucesso");
      navigate(landingFor(profile), { replace: true });
    } catch (error) {
      const code = error.response?.data?.code;
      if (code === "AUTH_001") {
        setErrors({ currentPassword: "Senha atual incorreta" });
        toast.error("Senha atual incorreta");
      } else if (code === "AUTH_002") {
        toast.error("Sessão expirou — entre novamente para trocar a senha");
        navigate("/login", { replace: true });
      } else if (code === "VAL_001") {
        toast.error("Verifique os campos e tente novamente");
      } else {
        toast.error("Erro inesperado — tente novamente em instantes");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

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
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <div className="space-y-2">
              <label htmlFor="currentPassword" className="text-sm font-medium">
                Senha atual (provisória)
              </label>
              <Input
                id="currentPassword"
                type="password"
                autoComplete="current-password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
              />
              {errors.currentPassword && (
                <p className="text-sm text-destructive">{errors.currentPassword}</p>
              )}
            </div>

            <div className="space-y-2">
              <label htmlFor="newPassword" className="text-sm font-medium">Nova senha</label>
              <Input
                id="newPassword"
                type="password"
                autoComplete="new-password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
              {errors.newPassword && (
                <p className="text-sm text-destructive">{errors.newPassword}</p>
              )}
            </div>

            <div className="space-y-2">
              <label htmlFor="confirmPassword" className="text-sm font-medium">
                Confirme a nova senha
              </label>
              <Input
                id="confirmPassword"
                type="password"
                autoComplete="new-password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
              {errors.confirmPassword && (
                <p className="text-sm text-destructive">{errors.confirmPassword}</p>
              )}
            </div>

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting ? "Salvando..." : "Trocar senha"}
            </Button>

            <p className="text-xs text-muted-foreground text-center">
              Mínimo 8 caracteres.
            </p>
          </form>
        </CardContent>

        <CardFooter className="text-xs text-muted-foreground justify-center">
          Senac PE — VALORA
        </CardFooter>
      </Card>
    </div>
  );
}
