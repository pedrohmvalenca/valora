export default function BalanceCard({ detail }) {
  const balance = detail.balance;
  const requested = detail.requestedHours;
  const remaining = balance?.remainingHours ?? 0;
  const max = balance?.maxHours ?? 0;
  const accumulated = balance?.accumulatedHours ?? 0;
  const fits = requested <= remaining;
  const totalAfter = accumulated + Math.min(requested, remaining);
  const progressPercent = max > 0 ? (accumulated / max) * 100 : 0;

  let badgeText;
  let badgeClass;
  if (remaining === 0) {
    badgeText = "🚫 Categoria esgotada";
    badgeClass = "bg-destructive/15 text-destructive border-destructive/30";
  } else if (fits) {
    badgeText = "✓ Cabe inteiro";
    badgeClass = "bg-success/15 text-success border-success/30";
  } else {
    badgeText = `⚠ Cabe parcial — só até ${remaining}h`;
    badgeClass = "bg-warning/15 text-warning border-warning/30";
  }

  let amountClass;
  if (remaining === 0) {
    amountClass = "text-destructive";
  } else if (fits) {
    amountClass = "text-success";
  } else {
    amountClass = "text-warning";
  }

  return (
    <div className="bg-muted/30 border border-border rounded-lg p-5">
      <div className="flex items-baseline justify-between mb-3">
        <p className="text-sm text-muted-foreground">Saldo restante na categoria/curso</p>
        <p className="text-xs text-muted-foreground">{detail.categoryName} · {detail.courseName}</p>
      </div>
      <div className="flex items-baseline gap-3 mb-3">
        <span className={`text-5xl font-bold tabular-nums leading-none ${amountClass}`}>{remaining}h</span>
        <span className="text-sm font-medium text-muted-foreground">restantes / pediu {requested}h</span>
      </div>
      <div className="mb-3">
        <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold border ${badgeClass}`}>
          {badgeText}
        </span>
      </div>
      <div className="w-full h-2 bg-muted rounded overflow-hidden">
        <div className="h-full bg-success transition-all" style={{ width: `${Math.min(100, progressPercent)}%` }} />
      </div>
      <div className="flex justify-between text-xs text-muted-foreground mt-1.5 tabular-nums">
        <span>{accumulated}h aprovadas</span>
        <span>limite {max}h</span>
      </div>
      {detail.status === "PENDING" && remaining > 0 && (
        <p className="text-xs text-muted-foreground mt-3">
          Após esta decisão: <span className="tabular-nums font-medium text-foreground">{totalAfter}h</span> /{" "}
          <span className="tabular-nums">{max}h</span>
        </p>
      )}
    </div>
  );
}
