import { useEffect } from "react";
import { ThemeProvider as NextThemesProvider, useTheme } from "next-themes";

/**
 * Wrapper next-themes:
 * - attribute="class" combina com tailwind.config darkMode: ["class"]
 * - defaultTheme="dark" implementa UX-DR9 (dark-first puro)
 * - storageKey="valora.theme" evita colisão com outras SPAs next-themes na mesma origem
 *
 * Decisão D1 do code review da Story 1.2 (2026-05-03): enableSystem REMOVIDO.
 * Razão: usuário com sistema=light veria o app em light na primeira visita,
 * contrariando UX-DR9 ("dark-first"). Agora dark é o padrão sempre; usuário
 * troca via ThemeToggle e a escolha persiste em localStorage["valora.theme"].
 */
export function ThemeProvider({ children }) {
  return (
    <NextThemesProvider
      attribute="class"
      defaultTheme="dark"
      storageKey="valora.theme"
      disableTransitionOnChange
    >
      <ThemeAttributeSync />
      {children}
    </NextThemesProvider>
  );
}

function ThemeAttributeSync() {
  const { resolvedTheme } = useTheme();
  useEffect(() => {
    if (typeof document !== "undefined" && resolvedTheme) {
      document.documentElement.setAttribute("data-theme", resolvedTheme);
    }
  }, [resolvedTheme]);
  return null;
}
