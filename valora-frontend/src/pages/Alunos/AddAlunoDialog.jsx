import { useEffect, useState } from "react";
import { toast } from "sonner";
import { ArrowLeft, Plus, Search } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import { studentsApi } from "@/services/admin";

const EMPTY_FORM = { registrationCode: "", name: "", email: "", courseIds: [] };

export default function AddAlunoDialog({ open, onOpenChange, courses, onSuccess }) {
  const [mode, setMode] = useState("search");
  const [query, setQuery] = useState("");
  const [results, setResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [selected, setSelected] = useState(null);
  const [linkCourseIds, setLinkCourseIds] = useState([]);
  const [linking, setLinking] = useState(false);

  const [form, setForm] = useState(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!open || mode !== "search") return;
    const term = query.trim();
    if (term.length < 2) { setResults([]); return; }
    setSearching(true);
    const t = setTimeout(async () => {
      try {
        setResults(await studentsApi.search(term));
      } catch {
        toast.error("Erro na busca");
      } finally {
        setSearching(false);
      }
    }, 300);
    return () => clearTimeout(t);
  }, [query, open, mode]);

  function resetDialog() {
    setMode("search");
    setQuery("");
    setResults([]);
    setSearching(false);
    setSelected(null);
    setLinkCourseIds([]);
    setForm(EMPTY_FORM);
  }

  function handleOpenChange(next) {
    onOpenChange(next);
    if (!next) resetDialog();
  }

  function toggleLinkCourse(id) {
    setLinkCourseIds((ids) => ids.includes(id) ? ids.filter((x) => x !== id) : [...ids, id]);
  }

  function toggleFormCourse(id) {
    setForm((f) => ({
      ...f,
      courseIds: f.courseIds.includes(id)
        ? f.courseIds.filter((x) => x !== id)
        : [...f.courseIds, id],
    }));
  }

  function selectResult(student) {
    setSelected(student);
    setLinkCourseIds([]);
  }

  function goCreate() {
    const term = query.trim();
    setForm((f) => ({
      ...f,
      name: term && !term.includes("@") ? term : f.name,
      email: term.includes("@") ? term : f.email,
    }));
    setSelected(null);
    setMode("create");
  }

  async function handleLink() {
    if (linkCourseIds.length === 0) { toast.error("Selecione ao menos 1 curso"); return; }
    setLinking(true);
    try {
      await studentsApi.linkCourses(selected.id, linkCourseIds);
      toast.success(`${selected.name} vinculado(a) ao(s) curso(s)`);
      handleOpenChange(false);
      await onSuccess();
    } catch (error) {
      toast.error(error.response?.data?.message || "Erro ao vincular aluno");
    } finally {
      setLinking(false);
    }
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!form.registrationCode.trim() || !form.name.trim() || !form.email.trim()) {
      toast.error("Preencha matrícula, nome e e-mail"); return;
    }
    if (form.courseIds.length === 0) { toast.error("Vincule ao menos 1 curso"); return; }
    setSubmitting(true);
    try {
      await studentsApi.create({
        registrationCode: form.registrationCode.trim(),
        name: form.name.trim(),
        email: form.email.trim().toLowerCase(),
        courseIds: form.courseIds,
      });
      toast.success(`Aluno "${form.name}" cadastrado`);
      handleOpenChange(false);
      await onSuccess();
    } catch (error) {
      toast.error(error.response?.data?.message || "Erro ao criar aluno");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogTrigger asChild>
        <Button><Plus className="h-4 w-4 mr-1" /> Adicionar aluno</Button>
      </DialogTrigger>
      <DialogContent>
        {mode === "search" ? (
          <>
            <DialogHeader>
              <DialogTitle>Adicionar aluno ao curso</DialogTitle>
              <DialogDescription>
                Busque primeiro — o aluno pode já existir em outro curso. Vincule o existente para não duplicar.
              </DialogDescription>
            </DialogHeader>

            <div className="space-y-4">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  autoFocus
                  value={query}
                  onChange={(e) => { setQuery(e.target.value); setSelected(null); }}
                  placeholder="Busque por matrícula, nome ou e-mail"
                  className="pl-9"
                />
              </div>

              <div className="border border-border rounded-md divide-y divide-border max-h-56 overflow-y-auto">
                {query.trim().length < 2 && (
                  <p className="text-xs text-muted-foreground p-3">Digite ao menos 2 caracteres para buscar.</p>
                )}
                {query.trim().length >= 2 && searching && (
                  <p className="text-xs text-muted-foreground p-3">Buscando…</p>
                )}
                {query.trim().length >= 2 && !searching && results.length === 0 && (
                  <p className="text-xs text-muted-foreground p-3">Nenhum aluno encontrado.</p>
                )}
                {!searching && results.map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => selectResult(s)}
                    className={`w-full text-left p-3 transition-colors ${
                      selected?.id === s.id ? "bg-primary/10" : "hover:bg-muted/50"
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-medium text-sm truncate">{s.name}</span>
                      <Badge variant={s.isActive ? "default" : "secondary"} className="text-xs shrink-0">
                        {s.isActive ? "Ativo" : "Inativo"}
                      </Badge>
                    </div>
                    <div className="text-xs text-muted-foreground flex items-center gap-1.5 flex-wrap mt-0.5">
                      <span className="font-mono">{s.registrationCode}</span>
                      <span className="text-border">·</span>
                      <span className="truncate">{s.email}</span>
                      <span className="text-border">·</span>
                      <span>em {s.linkedCourseCount} curso(s)</span>
                    </div>
                  </button>
                ))}
              </div>

              {selected && (
                <div className="border border-primary/30 rounded-md p-3 space-y-3 bg-primary/5">
                  <p className="text-sm">
                    Vincular <span className="font-medium">{selected.name}</span> a:
                  </p>
                  <div className="space-y-2 max-h-36 overflow-y-auto">
                    {courses.length === 0 && <p className="text-xs text-muted-foreground">Sem cursos disponíveis.</p>}
                    {courses.map((c) => (
                      <label key={c.id} className="flex items-center gap-2 cursor-pointer text-sm">
                        <Checkbox checked={linkCourseIds.includes(c.id)} onCheckedChange={() => toggleLinkCourse(c.id)} />
                        <span><span className="font-mono text-xs">{c.code}</span> — {c.name}</span>
                      </label>
                    ))}
                  </div>
                  <Button size="sm" onClick={handleLink} disabled={linking}>
                    {linking ? "Vinculando…" : "Vincular ao(s) curso(s)"}
                  </Button>
                </div>
              )}
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={goCreate}>
                <Plus className="h-4 w-4 mr-1" /> Não encontrou? Cadastrar novo aluno
              </Button>
            </DialogFooter>
          </>
        ) : (
          <>
            <DialogHeader>
              <DialogTitle>Cadastrar novo aluno</DialogTitle>
              <DialogDescription>Cria a identidade do aluno e vincula a um ou mais cursos.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <Label htmlFor="reg">Matrícula</Label>
                <Input id="reg" value={form.registrationCode}
                  onChange={(e) => setForm({ ...form, registrationCode: e.target.value })} placeholder="Ex.: 2026005" />
              </div>
              <div>
                <Label htmlFor="name">Nome</Label>
                <Input id="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Ex.: João Silva" />
              </div>
              <div>
                <Label htmlFor="email">E-mail</Label>
                <Input id="email" type="email" value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="joao@aluno.senac.local" />
              </div>
              <div>
                <Label>Cursos vinculados</Label>
                <div className="border border-border rounded-md p-3 space-y-2 max-h-48 overflow-y-auto">
                  {courses.length === 0 && <p className="text-xs text-muted-foreground">Sem cursos disponíveis.</p>}
                  {courses.map((c) => (
                    <label key={c.id} className="flex items-center gap-2 cursor-pointer text-sm">
                      <Checkbox checked={form.courseIds.includes(c.id)} onCheckedChange={() => toggleFormCourse(c.id)} />
                      <span><span className="font-mono text-xs">{c.code}</span> — {c.name}</span>
                    </label>
                  ))}
                </div>
              </div>
              <DialogFooter className="gap-2 sm:justify-between">
                <Button type="button" variant="ghost" onClick={() => setMode("search")}>
                  <ArrowLeft className="h-4 w-4 mr-1" /> Voltar para busca
                </Button>
                <Button type="submit" disabled={submitting}>{submitting ? "Cadastrando..." : "Cadastrar"}</Button>
              </DialogFooter>
            </form>
          </>
        )}
      </DialogContent>
    </Dialog>
  );
}
