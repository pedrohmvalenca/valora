import { useRef } from "react";
import { Search, X } from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from "@/components/ui/select";
import {
  Table, TableBody, TableCell, TableHead, TableHeader, TableRow,
} from "@/components/ui/table";

function matchesAluno(student, searchQuery, courseFilter) {
  const q = searchQuery.trim().toLowerCase();
  const matchesText =
    !q ||
    (student.name ?? "").toLowerCase().includes(q) ||
    (student.registrationCode ?? "").toLowerCase().includes(q) ||
    (student.email ?? "").toLowerCase().includes(q);
  const matchesCourse =
    !courseFilter ||
    (Array.isArray(student.linkedCourseIds) && student.linkedCourseIds.includes(courseFilter));
  return matchesText && matchesCourse;
}

function courseNames(ids, courses) {
  return ids.map((id) => courses.find((c) => c.id === id)?.code || "?").join(", ");
}

export default function AlunosTable({
  list,
  courses,
  loading,
  searchQuery,
  setSearchQuery,
  courseFilter,
  setCourseFilter,
}) {
  const searchInputRef = useRef(null);
  const filteredList = list.filter((s) => matchesAluno(s, searchQuery, courseFilter));

  function renderEmptyMessage() {
    if (searchQuery) {
      const isLikeEmail = searchQuery.includes("@");
      return (
        <>
          Nenhum aluno corresponde a <strong>"{searchQuery}"</strong> nos seus cursos. Talvez ele já exista em outro curso — use{" "}
          <strong>Adicionar aluno</strong> para buscar{" "}
          {isLikeEmail ? (
            <><strong>pelo e-mail</strong> entre todos</>
          ) : (
            "entre todos"
          )}.
        </>
      );
    }
    if (courseFilter) {
      const courseCode = courses.find((c) => c.id === courseFilter)?.code ?? "?";
      return (
        <>
          Nenhum aluno vinculado ao curso <strong>{courseCode}</strong> nos seus cursos.
        </>
      );
    }
    return null;
  }

  return (
    <>
      <div className="flex flex-col sm:flex-row gap-2">
        <div className="relative flex-1 sm:basis-3/5">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            ref={searchInputRef}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Buscar por matrícula, nome ou e-mail"
            aria-label="Buscar aluno por matrícula, nome ou e-mail"
            className="pl-9 pr-9"
          />
          {searchQuery.trim() && (
            <button
              type="button"
              onClick={() => { setSearchQuery(""); searchInputRef.current?.focus(); }}
              aria-label="Limpar busca"
              className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded-md text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>
        <Select
          value={courseFilter ?? "ALL"}
          onValueChange={(value) => setCourseFilter(value === "ALL" ? null : value)}
        >
          <SelectTrigger className="sm:basis-2/5" aria-label="Filtrar por curso">
            <SelectValue placeholder="Todos os cursos" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Todos os cursos</SelectItem>
            {courses.map((c) => (
              <SelectItem key={c.id} value={c.id}>
                <span className="font-mono text-xs">{c.code}</span> — {c.name}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
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
            {loading && (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">Carregando…</TableCell>
              </TableRow>
            )}
            {!loading && list.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground" role="status">
                  Nenhum aluno cadastrado/visível.
                </TableCell>
              </TableRow>
            )}
            {!loading && list.length > 0 && filteredList.length === 0 && (
              <TableRow key={`empty-${searchQuery}-${courseFilter ?? ""}`}>
                <TableCell colSpan={5} className="text-center py-8 text-muted-foreground" role="status">
                  {renderEmptyMessage()}
                </TableCell>
              </TableRow>
            )}
            {!loading && filteredList.map((s) => (
              <TableRow key={s.id}>
                <TableCell className="font-mono text-sm">{s.registrationCode}</TableCell>
                <TableCell className="font-medium">{s.name}</TableCell>
                <TableCell className="text-sm">{s.email}</TableCell>
                <TableCell className="text-sm font-mono">{courseNames(s.linkedCourseIds, courses)}</TableCell>
                <TableCell><Badge variant={s.isActive ? "default" : "secondary"}>{s.isActive ? "Ativo" : "Inativo"}</Badge></TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </div>
    </>
  );
}
