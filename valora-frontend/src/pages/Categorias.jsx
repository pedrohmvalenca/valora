import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Plus, Tags, Trash2 } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";

import { categoriesApi, coursesApi } from "@/services/admin";

const GROUP_LABELS = { TEACHING: "Ensino", RESEARCH: "Pesquisa", EXTENSION: "Extensão" };

export default function Categorias() {
  const [list, setList] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    name: "", groupType: "TEACHING", description: "",
    courseLimits: [{ courseId: "", maxHours: 60 }],
  });

  async function refresh() {
    setLoading(true);
    try {
      const [cats, crs] = await Promise.all([categoriesApi.list(), coursesApi.list()]);
      setList(cats); setCourses(crs);
    } catch {
      toast.error("Erro ao carregar categorias");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  function addLimit() {
    setForm((f) => ({ ...f, courseLimits: [...f.courseLimits, { courseId: "", maxHours: 60 }] }));
  }

  function removeLimit(idx) {
    setForm((f) => ({ ...f, courseLimits: f.courseLimits.filter((_, i) => i !== idx) }));
  }

  function setLimit(idx, field, value) {
    setForm((f) => ({
      ...f,
      courseLimits: f.courseLimits.map((l, i) => (i === idx ? { ...l, [field]: value } : l)),
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.name.trim()) { toast.error("Nome obrigatório"); return; }
    const limits = form.courseLimits.filter((l) => l.courseId && l.maxHours > 0);
    if (limits.length === 0) { toast.error("Defina ao menos 1 curso com limite"); return; }
    setSubmitting(true);
    try {
      await categoriesApi.create({
        name: form.name.trim(),
        groupType: form.groupType,
        description: form.description.trim() || null,
        courseLimits: limits.map((l) => ({
          courseId: l.courseId,
          maxHours: parseInt(l.maxHours, 10),
        })),
      });
      toast.success(`Categoria "${form.name}" cadastrada`);
      setDialogOpen(false);
      setForm({ name: "", groupType: "TEACHING", description: "",
        courseLimits: [{ courseId: "", maxHours: 60 }] });
      await refresh();
    } catch (error) {
      toast.error(error.response?.data?.message || "Erro ao criar categoria");
    } finally {
      setSubmitting(false);
    }
  }

  function fmtLimits(limits) {
    return limits.map((l) => {
      const c = courses.find((x) => x.id === l.courseId);
      return `${c?.code || "?"}: ${l.maxHours}h`;
    }).join(" · ");
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Tags className="h-6 w-6 text-muted-foreground" />
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Categorias & Regras</h1>
            <p className="text-sm text-muted-foreground">Categorias de atividades complementares com limite de horas por curso</p>
          </div>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button><Plus className="h-4 w-4 mr-1" /> Nova categoria</Button>
          </DialogTrigger>
          <DialogContent className="max-w-lg">
            <DialogHeader>
              <DialogTitle>Cadastrar categoria</DialogTitle>
              <DialogDescription>Defina o limite máximo de horas por curso.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <Label htmlFor="name">Nome</Label>
                  <Input id="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Monitoria" />
                </div>
                <div>
                  <Label htmlFor="group">Grupo</Label>
                  <Select value={form.groupType} onValueChange={(v) => setForm({ ...form, groupType: v })}>
                    <SelectTrigger><SelectValue /></SelectTrigger>
                    <SelectContent>
                      <SelectItem value="TEACHING">Ensino</SelectItem>
                      <SelectItem value="RESEARCH">Pesquisa</SelectItem>
                      <SelectItem value="EXTENSION">Extensão</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div>
                <Label htmlFor="desc">Descrição (opcional)</Label>
                <Textarea id="desc" value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} />
              </div>
              <div>
                <Label>Limites por curso</Label>
                <div className="space-y-2 mt-1">
                  {form.courseLimits.map((l, i) => (
                    <div key={i} className="flex items-center gap-2">
                      <Select value={l.courseId} onValueChange={(v) => setLimit(i, "courseId", v)}>
                        <SelectTrigger className="flex-1"><SelectValue placeholder="Curso" /></SelectTrigger>
                        <SelectContent>
                          {courses.map((c) => (
                            <SelectItem key={c.id} value={c.id}>{c.code} — {c.name}</SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <Input type="number" min="1" value={l.maxHours}
                        onChange={(e) => setLimit(i, "maxHours", e.target.value)}
                        className="w-24" placeholder="h" />
                      {form.courseLimits.length > 1 && (
                        <Button type="button" variant="ghost" size="icon" onClick={() => removeLimit(i)}>
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      )}
                    </div>
                  ))}
                  <Button type="button" variant="outline" size="sm" onClick={addLimit}>
                    <Plus className="h-3 w-3 mr-1" /> Adicionar curso
                  </Button>
                </div>
              </div>
              <DialogFooter>
                <Button type="submit" disabled={submitting}>{submitting ? "Cadastrando..." : "Cadastrar"}</Button>
              </DialogFooter>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <div className="border border-border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Nome</TableHead>
              <TableHead>Grupo</TableHead>
              <TableHead>Limites por curso</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && <TableRow><TableCell colSpan={3} className="text-center py-8 text-muted-foreground">Carregando…</TableCell></TableRow>}
            {!loading && list.length === 0 && <TableRow><TableCell colSpan={3} className="text-center py-8 text-muted-foreground">Nenhuma categoria cadastrada.</TableCell></TableRow>}
            {!loading && list.map((c) => (
              <TableRow key={c.id}>
                <TableCell className="font-medium">{c.name}</TableCell>
                <TableCell><span className="text-xs">{GROUP_LABELS[c.groupType]}</span></TableCell>
                <TableCell className="text-sm font-mono">{fmtLimits(c.courseLimits)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
