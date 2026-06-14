import { useEffect, useState } from "react";
import { toast } from "sonner";
import { GraduationCap } from "lucide-react";

import { coursesApi, studentsApi } from "@/services/admin";

import AddAlunoDialog from "@/pages/Alunos/AddAlunoDialog";
import AlunosTable from "@/pages/Alunos/AlunosTable";

export default function Alunos() {
  const [list, setList] = useState([]);
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [courseFilter, setCourseFilter] = useState(null);

  async function refresh() {
    setLoading(true);
    try {
      const [students, courseList] = await Promise.all([studentsApi.list(), coursesApi.list()]);
      setList(students);
      setCourses(courseList);
    } catch {
      toast.error("Erro ao carregar alunos");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { refresh(); }, []);

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
        <AddAlunoDialog
          open={dialogOpen}
          onOpenChange={setDialogOpen}
          courses={courses}
          onSuccess={refresh}
        />
      </div>

      <AlunosTable
        list={list}
        courses={courses}
        loading={loading}
        searchQuery={searchQuery}
        setSearchQuery={setSearchQuery}
        courseFilter={courseFilter}
        setCourseFilter={setCourseFilter}
      />
    </div>
  );
}
