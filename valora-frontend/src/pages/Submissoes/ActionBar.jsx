import { Check, X } from "lucide-react";

import { Button } from "@/components/ui/button";

export default function ActionBar({
  detail,
  rejectMode,
  actionLoading,
  onApprove,
  onApprovePartial,
  onStartReject,
  onConfirmReject,
  onCancelReject,
  rejectDisabled,
}) {
  const remaining = detail.balance?.remainingHours ?? 0;
  const fits = detail.requestedHours <= remaining;
  const canPartial = !fits && remaining > 0;

  if (rejectMode) {
    return (
      <div className="border-t border-border bg-background px-6 py-3 flex items-center justify-between gap-2 sticky bottom-0">
        <Button variant="ghost" size="sm" onClick={onCancelReject} disabled={actionLoading}>
          Cancelar
        </Button>
        <Button variant="destructive" onClick={onConfirmReject} disabled={actionLoading || rejectDisabled}>
          <X className="h-4 w-4 mr-1" />
          {actionLoading ? "Reprovando…" : "Confirmar reprovação"}
        </Button>
      </div>
    );
  }

  return (
    <div className="border-t border-border bg-background px-6 py-3 flex items-center justify-between gap-2 sticky bottom-0">
      <div className="text-xs text-muted-foreground">
        {fits && remaining > 0 && <>Aprovar adicionará {detail.requestedHours}h ao saldo do aluno.</>}
        {!fits && remaining > 0 && <>Não cabe inteiro. Use <strong>Ajustar p/ {remaining}h</strong> para aprovar parcial.</>}
        {remaining === 0 && <>Saldo zerado nesta categoria — não pode aprovar.</>}
      </div>
      <div className="flex items-center gap-2">
        {canPartial && (
          <Button variant="outline" onClick={onApprovePartial} disabled={actionLoading}>
            Ajustar p/ {remaining}h
          </Button>
        )}
        <Button variant="destructive" onClick={onStartReject} disabled={actionLoading}>
          <X className="h-4 w-4 mr-1" />
          Reprovar
        </Button>
        <Button onClick={onApprove} disabled={actionLoading || !fits || remaining === 0}>
          <Check className="h-4 w-4 mr-1" />
          {actionLoading ? "Aprovando…" : "Aprovar"}
        </Button>
      </div>
    </div>
  );
}
