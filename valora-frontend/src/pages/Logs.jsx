import { History } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function Logs() {
  return (
    <div className="max-w-2xl mx-auto py-8">
      <Card>
        <CardHeader className="flex flex-row items-center gap-3">
          <History className="h-6 w-6 text-muted-foreground" />
          <CardTitle>Logs de Auditoria</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-muted-foreground">
          <p>Trilha de auditoria de todas as ações relevantes (login, logout, decisões de submissão, cadastros).</p>
          <p>Endpoint API: <code className="text-xs bg-muted px-1 rounded">GET /api/v1/logs?action=...&entityType=...</code></p>
          <p>UI rica de listagem — path 2.0, a caminho. Por ora consultar via API ou direto no banco.</p>
          <p>Acesso: <strong>Administrador</strong>.</p>
        </CardContent>
      </Card>
    </div>
  );
}
