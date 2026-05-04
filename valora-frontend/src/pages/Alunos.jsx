import { useEffect, useState } from "react";
import { toast } from "sonner";
import { GraduationCap, Plus } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog, DialogContent, DialogDescription, DialogFooter,
  DialogHeader, DialogTitle, DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";

import { coursesApi, studentsApi } from "@/services/admin";

export default function Alunos() {
  const [list, setList] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ registrationCode: "", name: "", email: "", courseIds: [] });

  async function refresh() {
    setLoading(true);
    try {
      const [students, courses] = await Promise.all([studentsApi.list(), coursesApi.list()]);
      setList(students);
      setCourses(courses);
    } catch {
      toast.error("Erro ao carregar alunos");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  function toggleCourse(id) {
    setForm((f) => ({
      ...f,
      courseIds: f.courseIds.includes(id)
        ? f.courseIds.filter((x) => x !== id)
        : [...f.courseIds, id],
    }));
  }

  async function handleSubmit(e) {
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
      setDialogOpen(false);
      setForm({ registrationCode: "", name: "", email: "", courseIds: [] });
      await refresh();
    } catch (error) {
      toast.error(error.response?.data?.message || "Erro ao criar aluno");
    } finally {
      setSubmitting(false);
    }
  }

  function courseNames(ids) {
    return ids.map((id) => courses.find((c) => c.id === id)?.code || "?").join(", ");
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <GraduationCap className="h-6 w-6 text-muted-foreground" />
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Alunos</h1>
            <p className="text-sm text-muted-foreground">Cadastro de alunos com vínculo a um ou mais cursos</p>
          </div>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button><Plus className="h-4 w-4 mr-1" /> Novo aluno</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Cadastrar aluno</DialogTitle>
              <DialogDescription>Aluno multi-bacharel: vincule a 2 ou mais cursos.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
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
                      <Checkbox checked={form.courseIds.includes(c.id)} onCheckedChange={() => toggleCourse(c.id)} />
                      <span><span className="font-mono text-xs">{c.code}</span> — {c.name}</span>
                    </label>
                  ))}
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
              <TableHead>Matrícula</TableHead>
              <TableHead>Nome</TableHead>
              <TableHead>E-mail</TableHead>
              <TableHead>Cursos</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && <TableRow><TableCell colSpan={5} className="text-center py-8 text-muted-foreground">Carregando…</TableCell></TableRow>}
            {!loading && list.length === 0 && <TableRow><TableCell colSpan={5} className="text-center py-8 text-muted-foreground">Nenhum aluno cadastrado/visível.</TableCell></TableRow>}
            {!loading && list.map((s) => (
              <TableRow key={s.id}>
                <TableCell className="font-mono text-sm">{s.registrationCode}</TableCell>
                <TableCell className="font-medium">{s.name}</TableCell>
                <TableCell className="text-sm">{s.email}</TableCell>
                <TableCell className="text-sm font-mono">{courseNames(s.linkedCourseIds)}</TableCell>
                <TableCell><Badge variant={s.isActive ? "default" : "secondary"}>{s.isActive ? "Ativo" : "Inativo"}</Badge></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
