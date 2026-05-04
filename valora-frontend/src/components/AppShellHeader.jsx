import { Menu } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import { ThemeToggle } from "@/components/ThemeToggle";
import { AppShellSidebar } from "@/components/AppShellSidebar";

/**
 * Header sticky 56px com drawer mobile.
 *
 * Story 1.2 não implementa collapse desktop (apenas drawer mobile via Sheet).
 * O botão de toggle desktop foi removido no code review (P2): seu state morava
 * só no Header e não atingia a sidebar (sibling). Quando alguma story de polish
 * decidir collapse-to-60px, o estado deve viver em context dentro de <AppShell>
 * e a sidebar consome.
 */
export function AppShellHeader() {
  return (
    <header className="sticky top-0 z-30 h-14 flex items-center justify-between border-b bg-card px-4 shrink-0">
      <div className="flex items-center gap-3">
        {/* Mobile: drawer trigger (esconde em desktop, onde a sidebar fica fixa) */}
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="md:hidden" aria-label="Abrir menu">
              <Menu className="h-5 w-5" />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="p-0 w-[260px]">
            {/* Radix exige título acessível em Dialog.Content (P3 do code review) */}
            <SheetTitle className="sr-only">Menu de navegação</SheetTitle>
            <SheetDescription className="sr-only">
              Acesse Dashboard, Submissões, Cursos e demais áreas do VALORA.
            </SheetDescription>
            {/* asDrawer evita o `hidden md:flex` baked-in da sidebar (P1) */}
            <AppShellSidebar asDrawer className="border-r-0" />
          </SheetContent>
        </Sheet>

        {/* CursoSelector slot — Story 1.7 */}
        <div className="hidden md:block text-sm text-muted-foreground" aria-hidden>
          {/* placeholder: <CursoSelector /> aqui */}
        </div>
      </div>

      <div className="flex items-center gap-2">
        <ThemeToggle />
      </div>
    </header>
  );
}
