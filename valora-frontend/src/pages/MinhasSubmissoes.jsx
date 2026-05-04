import { FileText } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export default function MinhasSubmissoes() {
  return (
    <div className="max-w-2xl mx-auto py-8">
      <Card>
        <CardHeader className="flex flex-row items-center gap-3">
          <FileText className="h-6 w-6 text-muted-foreground" />
          <CardTitle>Minhas Submissões</CardTitle>
        </CardHeader>
        <CardContent className="space-y-2 text-sm text-muted-foreground">
          <p>Path 2.0 — a caminho.</p>
          <p>Acesso: <strong>Aluno</strong> (suas próprias submissões) e <strong>Administrador</strong> (visão investigativa).</p>
        </CardContent>
      </Card>
    </div>
  );
}
