import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Check, ChevronDown, ChevronRight, FileText, RotateCcw, X } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Textarea } from "@/components/ui/textarea";

import { useAuth } from "@/contexts/AuthContext";
import { Profile } from "@/lib/constants";
import {
  approveSubmission,
  getSubmissionDetail,
  listSubmissions,
  rejectSubmission,
  revertSubmission,
} from "@/services/submissions";

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

/**
 * Story 4.5 nível C — layout master-detail (lista esquerda + detalhe direita).
 * Saldo indicator rico + histórico do aluno + 3 botões na sticky bar (Aprovar /
 * Ajustar p/ Xh / Reprovar). Inspirado no ux-mockup-validation.html.
 */
export default function Submissoes() {
  const { profile } = useAuth();
  const isAdmin = profile === Profile.ADMINISTRATOR;
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [page, setPage] = useState({ content: [], totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState(null);
  const [detail, setDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [rejectMode, setRejectMode] = useState(false);
  const [rejectReason, setRejectReason] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);

  async function refresh(keepSelected = false) {
    setLoading(true);
    try {
      const data = await listSubmissions({
        status: statusFilter === "ALL" ? undefined : statusFilter,
        size: 50,
      });
      setPage(data);
      if (!keepSelected) {
        setSelectedId(null);
        setDetail(null);
      }
    } catch {
      toast.error("Erro ao carregar submissões");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  async function openDetail(id) {
    setSelectedId(id);
    setDetailLoading(true);
    setRejectMode(false);
    setRejectReason("");
    setHistoryOpen(false);
    try {
      const data = await getSubmissionDetail(id);
      setDetail(data);
    } catch {
      toast.error("Erro ao carregar detalhe");
      setSelectedId(null);
    } finally {
      setDetailLoading(false);
    }
  }

  async function handleApprove(recognizedHours = null) {
    if (!detail) return;
    setActionLoading(true);
    try {
      await approveSubmission(detail.id, recognizedHours);
      toast.success(
        recognizedHours != null
          ? `Submissão aprovada com ${recognizedHours}h (parcial)`
          : "Submissão aprovada",
      );
      await refresh();
    } catch (error) {
      const code = error.response?.data?.code;
      const msg = error.response?.data?.message;
      if (code === "BIZ_004") toast.error(msg || "Aprovação excede saldo");
      else if (code === "BIZ_007") toast.error("Submissão já foi decidida");
      else toast.error(msg ? `Erro: ${msg}` : "Erro inesperado ao aprovar");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleRevert() {
    if (!detail) return;
    if (!window.confirm("Reverter decisão (modo demo)? Submissão volta para Pendente.")) return;
    setActionLoading(true);
    try {
      await revertSubmission(detail.id);
      toast.success("Decisão revertida — submissão voltou a PENDING");
      await refresh();
      // Reabre detalhe atualizado
      await openDetail(detail.id);
    } catch (error) {
      const msg = error.response?.data?.message;
      toast.error(msg ? `Erro: ${msg}` : "Erro ao reverter decisão");
    } finally {
      setActionLoading(false);
    }
  }

  async function handleReject() {
    if (!detail || rejectReason.trim().length < 20) {
      toast.error("Motivo deve ter ao menos 20 caracteres");
      return;
    }
    setActionLoading(true);
    try {
      await rejectSubmission(detail.id, rejectReason.trim());
      toast.success("Submissão reprovada");
      await refresh();
    } catch (error) {
      const code = error.response?.data?.code;
      if (code === "VAL_001") toast.error("Motivo inválido — mínimo 20 caracteres");
      else if (code === "BIZ_007") toast.error("Submissão já foi decidida");
      else toast.error("Erro inesperado ao reprovar");
    } finally {
      setActionLoading(false);
    }
  }

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[minmax(320px,38%)_1fr] gap-4 h-[calc(100vh-7rem)]">
      {/* ============ LISTA ============ */}
      <section className="flex flex-col min-w-0">
        <div className="flex items-center justify-between gap-2 mb-3">
          <div>
            <h1 className="text-xl font-semibold tracking-tight">Submissões</h1>
            <p className="text-xs text-muted-foreground">{page.totalElements} no filtro</p>
          </div>
          <Select value={statusFilter} onValueChange={setStatusFilter}>
            <SelectTrigger className="w-32"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="PENDING">Pendentes</SelectItem>
              <SelectItem value="APPROVED">Aprovadas</SelectItem>
              <SelectItem value="REJECTED">Reprovadas</SelectItem>
              <SelectItem value="ALL">Todas</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="flex-1 overflow-y-auto space-y-2 pr-1">
          {loading && <p className="text-sm text-muted-foreground p-4">Carregando…</p>}
          {!loading && page.content.length === 0 && (
            <p className="text-sm text-muted-foreground p-4">Nenhuma submissão neste filtro.</p>
          )}
          {!loading && page.content.map((s) => (
            <button
              key={s.id}
              onClick={() => openDetail(s.id)}
              className={`w-full text-left p-3 rounded-md border transition-colors ${
                selectedId === s.id
                  ? "bg-primary/10 border-primary/40"
                  : "bg-card border-border hover:bg-muted/50"
              }`}
            >
              <div className="flex items-start justify-between gap-2 mb-1">
                <span className="font-medium text-sm truncate">{s.studentName}</span>
                <Badge variant={STATUS_VARIANTS[s.status]} className="text-xs shrink-0">
                  {STATUS_LABELS[s.status]}
                </Badge>
              </div>
              <div className="text-xs text-muted-foreground flex items-center gap-1.5 flex-wrap">
                <span className="font-mono">{s.studentRegistration}</span>
                <span className="text-border">·</span>
                <span>{s.categoryName}</span>
                <span className="text-border">·</span>
                <span className="tabular-nums font-medium text-foreground">{s.requestedHours}h</span>
              </div>
            </button>
          ))}
        </div>
      </section>

      {/* ============ DETALHE ============ */}
      <section className="flex flex-col min-w-0 bg-card border border-border rounded-lg overflow-hidden">
        {!selectedId && (
          <div className="flex-1 flex flex-col items-center justify-center text-center p-8 text-muted-foreground">
            <FileText className="h-12 w-12 mb-3 opacity-30" />
            <p className="text-sm">Selecione uma submissão na lista para ver o detalhe.</p>
          </div>
        )}

        {selectedId && detailLoading && (
          <div className="flex-1 flex items-center justify-center text-muted-foreground">Carregando…</div>
        )}

        {selectedId && !detailLoading && detail && (
          <>
            {/* Cabeçalho */}
            <div className="px-6 py-4 border-b border-border">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="text-xs text-muted-foreground mb-1 font-mono">
                    Submissão · {detail.id.slice(0, 8)}…
                  </div>
                  <h2 className="text-xl font-semibold">{detail.studentName}</h2>
                  <div className="flex items-center gap-2 mt-1.5 text-sm text-muted-foreground flex-wrap">
                    <span className="font-mono text-xs">{detail.studentRegistration}</span>
                    <span className="text-border">·</span>
                    <span>Curso: <span className="text-foreground font-medium">{detail.courseName}</span></span>
                    <span className="text-border">·</span>
                    <span>{detail.categoryName}</span>
                  </div>
                </div>
                <Badge variant={STATUS_VARIANTS[detail.status]} className="shrink-0">
                  {STATUS_LABELS[detail.status]}
                </Badge>
              </div>
            </div>

            {/* Body scrollable */}
            <div className="flex-1 overflow-y-auto px-6 py-5 space-y-5">
              {/* Saldo Indicator rico */}
              <BalanceCard detail={detail} />

              {/* Descrição */}
              <div>
                <p className="text-xs uppercase text-muted-foreground mb-1">Descrição</p>
                <p className="text-sm">{detail.description}</p>
              </div>

              {/* Comprovante (placeholder) */}
              {detail.proofPath && (
                <div>
                  <p className="text-xs uppercase text-muted-foreground mb-1">Comprovante</p>
                  <div className="border border-border rounded-md p-4 bg-muted/30 flex items-center gap-3">
                    <FileText className="h-6 w-6 text-muted-foreground" />
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-mono truncate">{detail.proofPath}</p>
                      <p className="text-xs text-muted-foreground">
                        Visualização inline — path 2.0, a caminho.
                      </p>
                    </div>
                  </div>
                </div>
              )}

              {/* Histórico colapsado */}
              <HistoryCard
                history={detail.history}
                isOpen={historyOpen}
                onToggle={() => setHistoryOpen((o) => !o)}
              />

              {/* Motivo de reprovação (se REJECTED) */}
              {detail.status === "REJECTED" && detail.rejectionReason && (
                <Card className="border-destructive/30">
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm text-destructive">Motivo de reprovação</CardTitle>
                  </CardHeader>
                  <CardContent className="text-sm">{detail.rejectionReason}</CardContent>
                </Card>
              )}

              {/* Resumo da decisão (se APPROVED) */}
              {detail.status === "APPROVED" && (
                <Card className="border-success/30 bg-success/5">
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm">Aprovada</CardTitle>
                  </CardHeader>
                  <CardContent className="text-sm">
                    Horas reconhecidas: <span className="font-semibold tabular-nums">{detail.recognizedHours}h</span>
                    {detail.recognizedHours < detail.requestedHours && (
                      <span className="text-muted-foreground"> (aprovação parcial — solicitado {detail.requestedHours}h)</span>
                    )}
                  </CardContent>
                </Card>
              )}

              {/* Reverter decisão — modo demo, Admin only */}
              {isAdmin && detail.status !== "PENDING" && (
                <div className="border border-dashed border-warning/50 rounded-md p-3 bg-warning/5">
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-semibold text-warning mb-0.5">⚠ Modo demo</p>
                      <p className="text-xs text-muted-foreground">
                        Reverter volta a submissão para Pendente. Em produção decisões são imutáveis.
                      </p>
                    </div>
                    <Button variant="outline" size="sm" onClick={handleRevert} disabled={actionLoading}>
                      <RotateCcw className="h-3.5 w-3.5 mr-1" />
                      Reverter
                    </Button>
                  </div>
                </div>
              )}

              {/* Reject mode inline */}
              {detail.status === "PENDING" && rejectMode && (
                <Card className="border-destructive/30">
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm">Motivo da reprovação</CardTitle>
                    <p className="text-xs text-muted-foreground">Mínimo 20 caracteres</p>
                  </CardHeader>
                  <CardContent className="space-y-2">
                    <Textarea
                      value={rejectReason}
                      onChange={(e) => setRejectReason(e.target.value)}
                      placeholder="Ex.: Atividade não atende aos critérios institucionais…"
                      rows={4}
                    />
                    <p className="text-xs text-muted-foreground tabular-nums">{rejectReason.trim().length}/20</p>
                  </CardContent>
                </Card>
              )}
            </div>

            {/* Sticky Action Bar */}
            {detail.status === "PENDING" && (
              <ActionBar
                detail={detail}
                rejectMode={rejectMode}
                actionLoading={actionLoading}
                onApprove={() => handleApprove()}
                onApprovePartial={() => handleApprove(detail.balance.remainingHours)}
                onStartReject={() => setRejectMode(true)}
                onConfirmReject={handleReject}
                onCancelReject={() => { setRejectMode(false); setRejectReason(""); }}
                rejectDisabled={rejectReason.trim().length < 20}
              />
            )}
          </>
        )}
      </section>
    </div>
  );
}

// ============ BalanceCard ============
function BalanceCard({ detail }) {
  const balance = detail.balance;
  const requested = detail.requestedHours;
  const remaining = balance?.remainingHours ?? 0;
  const max = balance?.maxHours ?? 0;
  const accumulated = balance?.accumulatedHours ?? 0;
  const fits = requested <= remaining;
  const totalAfter = accumulated + Math.min(requested, remaining);
  const progressPercent = max > 0 ? (accumulated / max) * 100 : 0;

  let badgeText, badgeClass;
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

  return (
    <div className="bg-muted/30 border border-border rounded-lg p-5">
      <div className="flex items-baseline justify-between mb-3">
        <p className="text-sm text-muted-foreground">Saldo restante na categoria/curso</p>
        <p className="text-xs text-muted-foreground">{detail.categoryName} · {detail.courseName}</p>
      </div>
      <div className="flex items-baseline gap-3 mb-3">
        <span className={`text-5xl font-bold tabular-nums leading-none ${
          remaining === 0 ? "text-destructive" : fits ? "text-success" : "text-warning"
        }`}>{remaining}h</span>
        <span className="text-sm font-medium text-muted-foreground">restantes / pediu {requested}h</span>
      </div>
      <div className="mb-3">
        <span className={`inline-block px-2 py-0.5 rounded text-xs font-semibold border ${badgeClass}`}>
          {badgeText}
        </span>
      </div>
      {/* Progress bar */}
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

// ============ HistoryCard ============
function HistoryCard({ history, isOpen, onToggle }) {
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

// ============ ActionBar ============
function ActionBar({
  detail, rejectMode, actionLoading,
  onApprove, onApprovePartial, onStartReject, onConfirmReject, onCancelReject,
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
