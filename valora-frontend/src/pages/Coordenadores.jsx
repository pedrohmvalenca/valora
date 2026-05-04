import { useEffect, useState } from "react";
import { toast } from "sonner";
import { Plus, Users } from "lucide-react";

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

import { coordinatorsApi, coursesApi } from "@/services/admin";

export default function Coordenadores() {
  const [list, setList] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ name: "", email: "", courseIds: [] });

  async function refresh() {
    setLoading(true);
    try {
      const [coords, courses] = await Promise.all([coordinatorsApi.list(), coursesApi.list()]);
      setList(coords);
      setCourses(courses);
    } catch {
      toast.error("Erro ao carregar coordenadores");
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
    if (!form.name.trim() || !form.email.trim()) { toast.error("Nome e e-mail obrigatórios"); return; }
    if (form.courseIds.length === 0) { toast.error("Vincule ao menos 1 curso"); return; }
    setSubmitting(true);
    try {
      await coordinatorsApi.create({
        name: form.name.trim(), email: form.email.trim().toLowerCase(),
        courseIds: form.courseIds,
      });
      toast.success(`Coordenador "${form.name}" cadastrado (senha inicial: Admin@123)`);
      setDialogOpen(false);
      setForm({ name: "", email: "", courseIds: [] });
      await refresh();
    } catch (error) {
      toast.error(error.response?.data?.message || "Erro ao criar coordenador");
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
          <Users className="h-6 w-6 text-muted-foreground" />
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Coordenadores</h1>
            <p className="text-sm text-muted-foreground">Cadastro de coordenadores e vínculos com cursos</p>
          </div>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button><Plus className="h-4 w-4 mr-1" /> Novo coordenador</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Cadastrar coordenador</DialogTitle>
              <DialogDescription>Senha inicial padrão: <strong>Admin@123</strong> (trocar no primeiro acesso — path 2.0, a caminho).</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <Label htmlFor="name">Nome</Label>
                <Input id="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Ex.: Maria Souza" />
              </div>
              <div>
                <Label htmlFor="email">E-mail institucional</Label>
                <Input id="email" type="email" value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="maria.souza@valora.local" />
              </div>
              <div>
                <Label>Cursos vinculados</Label>
                <div className="border border-border rounded-md p-3 space-y-2 max-h-48 overflow-y-auto">
                  {courses.length === 0 && <p className="text-xs text-muted-foreground">Sem cursos cadastrados — cadastre em /cursos primeiro.</p>}
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
              <TableHead>Nome</TableHead>
              <TableHead>E-mail</TableHead>
              <TableHead>Cursos</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && <TableRow><TableCell colSpan={4} className="text-center py-8 text-muted-foreground">Carregando…</TableCell></TableRow>}
            {!loading && list.length === 0 && <TableRow><TableCell colSpan={4} className="text-center py-8 text-muted-foreground">Nenhum coordenador cadastrado.</TableCell></TableRow>}
            {!loading && list.map((c) => (
              <TableRow key={c.id}>
                <TableCell className="font-medium">{c.name}</TableCell>
                <TableCell className="text-sm">{c.email}</TableCell>
                <TableCell className="text-sm font-mono">{courseNames(c.linkedCourseIds)}</TableCell>
                <TableCell><Badge variant={c.isActive ? "default" : "secondary"}>{c.isActive ? "Ativo" : "Inativo"}</Badge></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
