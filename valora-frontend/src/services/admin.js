import { api } from "@/services/api";

/**
 * Wrappers dos endpoints admin (Cursos / Coordenadores / Alunos / Categorias).
 * Tier B+C+D+E do batch γ — list + create por entidade.
 */

// ============ Cursos (Admin only) ============
export const coursesApi = {
  list: async () => (await api.get("/courses")).data,
  create: async ({ name, code, minimumWorkloadHours = 100 }) =>
    (await api.post("/courses", { name, code, minimumWorkloadHours })).data,
};

// ============ Coordenadores (Admin only) ============
export const coordinatorsApi = {
  list: async () => (await api.get("/coordinators")).data,
  create: async ({ name, email, courseIds }) =>
    (await api.post("/coordinators", { name, email, courseIds })).data,
};

// ============ Alunos (Coord+Admin) ============
export const studentsApi = {
  list: async () => (await api.get("/students")).data,
  create: async ({ registrationCode, name, email, courseIds }) =>
    (await api.post("/students", { registrationCode, name, email, courseIds })).data,
};

// ============ Categorias (Coord+Admin) ============
export const categoriesApi = {
  list: async () => (await api.get("/categories")).data,
  create: async ({ name, groupType, description, courseLimits }) =>
    (await api.post("/categories", { name, groupType, description, courseLimits })).data,
};
