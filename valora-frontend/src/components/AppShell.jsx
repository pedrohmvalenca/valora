import { AppShellSidebar } from "@/components/AppShellSidebar";
import { AppShellHeader } from "@/components/AppShellHeader";
import { AppShellContent } from "@/components/AppShellContent";

/**
 * Layout composable conforme UX § AppShell.
 *
 * Uso:
 *   <AppShell>
 *     <AppShell.Sidebar />
 *     <AppShell.Main>
 *       <AppShell.Header />
 *       <AppShell.Content />
 *     </AppShell.Main>
 *   </AppShell>
 */
export function AppShell({ children }) {
  return <div className="min-h-screen flex w-full bg-background text-foreground">{children}</div>;
}

function AppShellMain({ children }) {
  return <div className="flex-1 flex flex-col min-w-0">{children}</div>;
}

AppShell.Sidebar = AppShellSidebar;
AppShell.Main = AppShellMain;
AppShell.Header = AppShellHeader;
AppShell.Content = AppShellContent;
