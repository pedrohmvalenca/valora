import { useEffect, useState } from "react";
import { toast } from "sonner";
import { LayoutDashboard, Printer } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";

import { coursesApi, dashboardApi } from "@/services/admin";
import { useAuth } from "@/contexts/AuthContext";
import { Profile } from "@/lib/constants";

const ALL_COURSES = "ALL";
const ALL_PERIODS = "ALL";

export default function Dashboard() {
  const { user, profile } = useAuth();
  const isAdmin = profile === Profile.ADMINISTRATOR;

  const [courses, setCourses] = useState([]);
  const [courseFilter, setCourseFilter] = useState(ALL_COURSES);
  const [periodFilter, setPeriodFilter] = useState(ALL_PERIODS);
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    async function loadCourses() {
      try {
        const list = await coursesApi.list();
        if (isAdmin) {
          setCourses(list);
        } else {
          const linkedIds = (Array.isArray(user?.linkedCourses) ? user.linkedCourses : [])
            .map((c) => c?.id ?? c);
          setCourses(list.filter((c) => linkedIds.includes(c.id)));
        }
      } catch {
        toast.error("Erro ao carregar cursos");
      }
    }
    loadCourses();
  }, [isAdmin, user]);

  useEffect(() => {
    async function loadDashboard() {
      setLoading(true);
      try {
        const params = courseFilter === ALL_COURSES ? {} : { courseId: courseFilter };
        const data = await dashboardApi.coordinator(params);
        setRows(Array.isArray(data) ? data : []);
      } catch {
        toast.error("Erro ao carregar dashboard");
        setRows([]);
      } finally {
        setLoading(false);
      }
    }
    loadDashboard();
  }, [courseFilter]);

  const sortedRows = [...rows].sort(
    (a, b) => (b.horasReconhecidas ?? 0) - (a.horasReconhecidas ?? 0),
  );
  const totalHours = sortedRows.reduce((sum, r) => sum + (r.horasReconhecidas ?? 0), 0);

  const showCourseFilter = courses.length > 1;

  function handlePrint() {
    window.print();
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <LayoutDashboard className="h-6 w-6 text-muted-foreground" />
          <div>
            <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
            <p className="text-sm text-muted-foreground">
              Horas reconhecidas por tipo de atividade
            </p>
          </div>
        </div>
        <Button variant="outline" onClick={handlePrint} className="print:hidden">
          <Printer className="h-4 w-4 mr-2" />
          Imprimir
        </Button>
      </div>

      <div className="flex flex-col sm:flex-row gap-2 print:hidden">
        {showCourseFilter && (
          <Select value={courseFilter} onValueChange={setCourseFilter}>
            <SelectTrigger className="sm:basis-1/2" aria-label="Filtrar por curso">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value={ALL_COURSES}>
                {isAdmin ? "Todos os cursos" : "Todos os cursos do Coord"}
              </SelectItem>
              {courses.map((c) => (
                <SelectItem key={c.id} value={c.id}>
                  <span className="font-mono text-xs">{c.code}</span> — {c.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )}
        <Select value={periodFilter} onValueChange={setPeriodFilter} disabled>
          <SelectTrigger className="sm:basis-1/2" aria-label="Filtrar por período semestral">
            <SelectValue placeholder="Período semestral (em breve)" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value={ALL_PERIODS}>Tudo</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="border border-border rounded-lg">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Tipo de atividade</TableHead>
              <TableHead className="text-right">Horas reconhecidas</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading && (
              <TableRow>
                <TableCell colSpan={2} className="text-center py-8 text-muted-foreground">
                  Carregando…
                </TableCell>
              </TableRow>
            )}
            {!loading && sortedRows.length === 0 && (
              <TableRow>
                <TableCell colSpan={2} className="text-center py-8 text-muted-foreground" role="status">
                  Nenhuma atividade reconhecida ainda.
                </TableCell>
              </TableRow>
            )}
            {!loading && sortedRows.map((r) => (
              <TableRow key={r.categoria}>
                <TableCell className="font-medium">{r.categoria}</TableCell>
                <TableCell className="text-right tabular-nums">
                  {r.horasReconhecidas ?? 0}h
                </TableCell>
              </TableRow>
            ))}
            {!loading && sortedRows.length > 0 && (
              <TableRow className="border-t-2 border-border bg-muted/30">
                <TableCell className="font-bold">Total geral</TableCell>
                <TableCell className="text-right font-bold tabular-nums">{totalHours}h</TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
