import { useEffect, useState } from "react";
import { Moon, Sun } from "lucide-react";
import { useTheme } from "next-themes";
import { Button } from "@/components/ui/button";

export function ThemeToggle() {
  const { theme, setTheme, resolvedTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => setMounted(true), []);

  // Antes do mount, resolvedTheme é undefined; renderiza placeholder com mesma forma
  // do botão real para evitar layout shift e label/ícone errado no first paint.
  if (!mounted) {
    return (
      <Button
        variant="ghost"
        size="icon"
        className="relative"
        aria-label="Alternar tema"
        disabled
      >
        <Sun className="h-5 w-5 opacity-0" />
      </Button>
    );
  }

  const current = theme === "system" ? resolvedTheme : theme;
  const next = current === "dark" ? "light" : "dark";

  return (
    <Button
      variant="ghost"
      size="icon"
      className="relative"
      onClick={() => setTheme(next)}
      aria-label={`Alternar para tema ${next === "dark" ? "escuro" : "claro"}`}
      title={`Tema atual: ${current === "dark" ? "escuro" : "claro"}`}
    >
      <Sun className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
      <Moon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
    </Button>
  );
}
