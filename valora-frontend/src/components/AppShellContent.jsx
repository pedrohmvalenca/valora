import { Outlet } from "react-router-dom";

export function AppShellContent() {
  return (
    <main className="flex-1 p-4 md:p-6 overflow-auto">
      <Outlet />
    </main>
  );
}
