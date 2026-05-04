import { useEffect, useState } from "react";
import { toast } from "sonner";
import { BookOpen, Plus } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

import { coursesApi } from "@/services/admin";

export default function Cursos() {
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({ name: "", code: "", minimumWorkloadHours: 100 });

  async function refresh() {
    setLoading(true);
    try {
      setList(await coursesApi.list());
    } catch {
      toast.error("Erro ao carregar cursos");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!form.name.trim() || !form.code.trim()) {
      toast.error("Preencha nome e código");
      return;
    }
    setSubmitting(true);
    try {
      await coursesApi.create({
        name: form.name.trim(),
        code: form.code.trim().toUpperCase(),
        minimumWorkloadHours: parseInt(form.minimumWorkloadHours, 10) || 100,
      });
      toast.success(`Curso "${form.name}" cadastrado`);
      setDialogOpen(false);
      setForm({ name: "", code: "", minimumWorkloadHours: 100 });
      await refresh();
    } catch (error) {
      toast.error(error.response?.data?.message || "Erro ao criar curso");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <BookOpen className="h-6 w-6 text-muted-foreground" />
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Cursos</h1>
            <p className="text-sm text-muted-foreground">Cadastro e gestão de cursos do Senac PE</p>
          </div>
        </div>
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
          <DialogTrigger asChild>
            <Button><Plus className="h-4 w-4 mr-1" /> Novo curso</Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Cadastrar novo curso</DialogTitle>
              <DialogDescription>Curso ficará ativo por padrão.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <Label htmlFor="name">Nome</Label>
                <Input id="name" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="Ex.: Análise e Desenvolvimento de Sistemas" />
              </div>
              <div>
                <Label htmlFor="code">Código</Label>
                <Input id="code" value={form.code} onChange={(e) => setForm({ ...form, code: e.target.value })} placeholder="Ex.: ADS-2026" />
              </div>
              <div>
                <Label htmlFor="hours">Carga horária mínima (AC)</Label>
                <Input id="hours" type="number" min="1" value={form.minimumWorkloadHours}
                  onChange={(e) => setForm({ ...form, minimumWorkloadHours: e.target.value })} />
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
              <TableHead>Código</TableHead>
              <TableHead className="text-right">CH mín.</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && <TableRow><TableCell colSpan={4} className="text-center py-8 text-muted-foreground">Carregando…</TableCell></TableRow>}
            {!loading && list.length === 0 && <TableRow><TableCell colSpan={4} className="text-center py-8 text-muted-foreground">Nenhum curso cadastrado.</TableCell></TableRow>}
            {!loading && list.map((c) => (
              <TableRow key={c.id}>
                <TableCell className="font-medium">{c.name}</TableCell>
                <TableCell className="font-mono text-sm">{c.code}</TableCell>
                <TableCell className="text-right tabular-nums">{c.minimumWorkloadHours}h</TableCell>
                <TableCell>
                  <Badge variant={c.isActive ? "default" : "secondary"}>{c.isActive ? "Ativo" : "Inativo"}</Badge>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
