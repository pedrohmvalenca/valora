import { ChevronDown, ChevronRight } from "lucide-react";

import { Badge } from "@/components/ui/badge";

const STATUS_LABELS = {
  PENDING: "Pendente",
  APPROVED: "Aprovada",
  REJECTED: "Reprovada",
};

const STATUS_VARIANTS = {
  PENDING: "secondary",
  APPROVED: "default",
  REJECTED: "destructive",
};

export default function HistoryCard({ history, isOpen, onToggle }) {
  const total = history?.length ?? 0;
  const approved = history?.filter((h) => h.status === "APPROVED").length ?? 0;
  const pct = total > 0 ? Math.round((approved / total) * 100) : 0;

  return (
    <div className="bg-card border border-border rounded-lg overflow-hidden">
      <button
        type="button"
        onClick={onToggle}
        className="w-full flex items-center justify-between px-4 py-3 hover:bg-muted/40 transition-colors"
      >
        <div className="flex items-center gap-2 text-sm">
          {isOpen ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
          <span className="font-medium">Histórico do aluno (mesma categoria/curso)</span>
          <span className="text-xs text-muted-foreground">
            {total} anteriores · {approved} aprovadas {total > 0 && `(${pct}%)`}
          </span>
        </div>
      </button>
      {isOpen && total > 0 && (
        <div className="border-t border-border divide-y divide-border">
          {history.map((h) => (
            <div key={h.id} className="px-4 py-2 text-xs flex items-center justify-between gap-3">
              <span className="truncate flex-1 min-w-0">{h.description}</span>
              <span className="tabular-nums text-muted-foreground shrink-0">
                {h.requestedHours}h
                {h.recognizedHours != null && h.recognizedHours !== h.requestedHours && (
                  <span> → {h.recognizedHours}h</span>
                )}
              </span>
              <Badge variant={STATUS_VARIANTS[h.status]} className="text-[10px] shrink-0">
                {STATUS_LABELS[h.status]}
              </Badge>
            </div>
          ))}
        </div>
      )}
      {isOpen && total === 0 && (
        <div className="border-t border-border px-4 py-3 text-xs text-muted-foreground">
          Sem histórico anterior nesta categoria/curso.
        </div>
      )}
    </div>
  );
}
