import { api } from "@/services/api";

export const coursesApi = {
  list: async () => (await api.get("/courses")).data,
  create: async ({ name, code, minimumWorkloadHours = 100 }) =>
    (await api.post("/courses", { name, code, minimumWorkloadHours })).data,
};

export const coordinatorsApi = {
  list: async () => (await api.get("/coordinators")).data,
  create: async ({ name, email, courseIds }) =>
    (await api.post("/coordinators", { name, email, courseIds })).data,
};

export const studentsApi = {
  list: async () => (await api.get("/students")).data,
  create: async ({ registrationCode, name, email, courseIds }) =>
    (await api.post("/students", { registrationCode, name, email, courseIds })).data,
  search: async (q) => (await api.get("/students/search", { params: { q } })).data,
  listCourses: async (studentId) =>
    (await api.get(`/students/${studentId}/courses`)).data,
  linkCourses: async (studentId, courseIds) =>
    (await api.post(`/students/${studentId}/courses`, { courseIds })).data,
};

export const categoriesApi = {
  list: async () => (await api.get("/categories")).data,
  create: async ({ name, groupType, description, courseLimits }) =>
    (await api.post("/categories", { name, groupType, description, courseLimits })).data,
};
