import { NavLink, useNavigate } from "react-router-dom";
import {
  BookOpen,
  FileCheck,
  FileText,
  GraduationCap,
  History,
  Inbox,
  LayoutDashboard,
  LogOut,
  Tags,
  Users,
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { Profile } from "@/lib/constants";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { cn } from "@/lib/utils";

// Items da sidebar VALORA — UX § Sidebar permanente.
// "Minhas Submissões" inclui ADMINISTRATOR intencionalmente (D3 do code review da Story 1.2):
// admin precisa investigar problemas em submissões; visão admin terá distintivo
// visual (badge/banner indicando modo admin).
// TODO Story 5.1: renderizar variant visual distintivo quando profile === ADMINISTRATOR
// (admin investigando submissões de aluno, não as próprias).
const ITEMS = [
  { to: "/", label: "Dashboard", Icon: LayoutDashboard, roles: [Profile.ADMINISTRATOR, Profile.COORDINATOR, Profile.STUDENT] },
  { to: "/pendencias", label: "Aguardando decisão", Icon: Inbox, roles: [Profile.ADMINISTRATOR, Profile.COORDINATOR] },
  { to: "/cursos", label: "Cursos", Icon: BookOpen, roles: [Profile.ADMINISTRATOR] },
  { to: "/coordenadores", label: "Coordenadores", Icon: Users, roles: [Profile.ADMINISTRATOR] },
  { to: "/alunos", label: "Alunos", Icon: GraduationCap, roles: [Profile.ADMINISTRATOR, Profile.COORDINATOR] },
  { to: "/categorias", label: "Categorias & Regras", Icon: Tags, roles: [Profile.ADMINISTRATOR, Profile.COORDINATOR] },
  { to: "/submissoes", label: "Submissões", Icon: FileCheck, roles: [Profile.ADMINISTRATOR, Profile.COORDINATOR] },
  { to: "/minhas-submissoes", label: "Minhas Submissões", Icon: FileText, roles: [Profile.ADMINISTRATOR, Profile.STUDENT] },
  { to: "/logs", label: "Logs", Icon: History, roles: [Profile.ADMINISTRATOR] },
];

/**
 * @param {object} props
 * @param {string} [props.className]
 * @param {boolean} [props.asDrawer] Quando true, NÃO aplica `hidden md:flex`
 *   (sidebar usada dentro de Sheet/drawer mobile precisa estar sempre visível
 *   dentro do seu wrapper). P1 do code review da Story 1.2.
 */
export function AppShellSidebar({ className, asDrawer = false }) {
  const { user, profile, logout } = useAuth();
  const navigate = useNavigate();
  const visibleItems = profile ? ITEMS.filter((item) => item.roles.includes(profile)) : ITEMS;

  async function handleLogout() {
    await logout();
    navigate("/login", { replace: true });
  }

  // Fallback robusto: name pode ser empty string (não capturado por ?? null/undefined)
  const name = (user?.name ?? "").trim() || "Visitante";
  const initial = (name.charAt(0) || "?").toUpperCase();

  return (
    <aside
      className={cn(
        // Base: sempre flex-col + tema sidebar
        "flex flex-col bg-sidebar text-sidebar-foreground",
        // Modo desktop fixo: 240px + border + esconde em mobile
        !asDrawer && "hidden md:flex w-[240px] shrink-0 border-r border-sidebar-border",
        // Modo drawer (Sheet): largura total do parent
        asDrawer && "w-full h-full",
        className,
      )}
      aria-label="Navegação principal"
    >
      {/* Topo: wordmark VALORA */}
      <div className="h-20 flex items-center px-5 border-b border-sidebar-border">
        <span className="text-lg font-bold tracking-tight">VALORA</span>
      </div>

      {/* Items de navegação */}
      <nav className="flex-1 py-4 overflow-y-auto">
        <ul className="space-y-0.5 px-2">
          {visibleItems.map(({ to, label, Icon }) => (
            <li key={to}>
              <NavLink
                to={to}
                end={to === "/"}
                className={({ isActive }) =>
                  cn(
                    "flex items-center gap-3 px-3 py-2 text-sm rounded-md transition-colors",
                    "hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
                    isActive &&
                      "bg-primary/15 text-sidebar-foreground border-l-2 border-l-primary pl-[10px] font-medium",
                  )
                }
              >
                <Icon className="h-[18px] w-[18px] shrink-0" />
                <span className="flex-1 text-left">{label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>

      {/* Rodapé: avatar + dropdown placeholder + nota Senac */}
      <div className="border-t border-sidebar-border p-3 space-y-2">
        <DropdownMenu>
          <DropdownMenuTrigger className="w-full flex items-center gap-3 px-2 py-2 rounded-md hover:bg-sidebar-accent transition-colors">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="text-xs bg-sidebar-accent text-sidebar-accent-foreground">
                {initial}
              </AvatarFallback>
            </Avatar>
            <span className="flex-1 text-sm text-left truncate">{name}</span>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>Conta</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem disabled>Trocar senha (path 2.0 — a caminho)</DropdownMenuItem>
            <DropdownMenuItem onSelect={handleLogout}>
              <LogOut className="mr-2 h-4 w-4" />
              Sair
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
        <p className="text-xs text-sidebar-foreground/60 px-2 pt-1">
          Senac PE — Projeto Integrador ADS-3
        </p>
      </div>
    </aside>
  );
}
