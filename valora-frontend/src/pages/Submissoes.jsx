import { useEffect, useState } from "react";
import { toast } from "sonner";
import { FileText, RotateCcw } from "lucide-react";

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

import ActionBar from "@/pages/Submissoes/ActionBar";
import BalanceCard from "@/pages/Submissoes/BalanceCard";
import HistoryCard from "@/pages/Submissoes/HistoryCard";
import ProofViewer from "@/pages/Submissoes/ProofViewer";

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
    async function load() {
      setLoading(true);
      try {
        const data = await listSubmissions({
          status: statusFilter === "ALL" ? undefined : statusFilter,
          size: 50,
        });
        setPage(data);
        setSelectedId(null);
        setDetail(null);
      } catch {
        toast.error("Erro ao carregar submissões");
      } finally {
        setLoading(false);
      }
    }
    load();
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
          {!loading && page.content.map((s) => {
            const itemClass = selectedId === s.id
              ? "bg-primary/10 border-primary/40"
              : "bg-card border-border hover:bg-muted/50";
            return (
              <button
                key={s.id}
                onClick={() => openDetail(s.id)}
                className={`w-full text-left p-3 rounded-md border transition-colors ${itemClass}`}
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
            );
          })}
        </div>
      </section>

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

            <div className="flex-1 overflow-y-auto px-6 py-5 space-y-5">
              <BalanceCard detail={detail} />

              <div>
                <p className="text-xs uppercase text-muted-foreground mb-1">Descrição</p>
                <p className="text-sm">{detail.description}</p>
              </div>

              {detail.proofPath && (
                <ProofViewer submissionId={detail.id} proofPath={detail.proofPath} />
              )}

              <HistoryCard
                history={detail.history}
                isOpen={historyOpen}
                onToggle={() => setHistoryOpen((o) => !o)}
              />

              {detail.status === "REJECTED" && detail.rejectionReason && (
                <Card className="border-destructive/30">
                  <CardHeader className="pb-2">
                    <CardTitle className="text-sm text-destructive">Motivo de reprovação</CardTitle>
                  </CardHeader>
                  <CardContent className="text-sm">{detail.rejectionReason}</CardContent>
                </Card>
              )}

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
