import { Inbox } from "lucide-react";
import { Link } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function Pendencias() {
  return (
    <div className="max-w-2xl mx-auto py-8">
      <Card>
        <CardHeader className="flex flex-row items-center gap-3">
          <Inbox className="h-6 w-6 text-muted-foreground" />
          <CardTitle>Aguardando decisão</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-muted-foreground">
          <p>Atalho para submissões aguardando decisão. Use a página <Link to="/submissoes" className="underline text-primary">Submissões</Link> com filtro <em>Pendentes</em>.</p>
          <p>Acesso: <strong>Coordenador</strong> e <strong>Administrador</strong>.</p>
        </CardContent>
      </Card>
    </div>
  );
}
