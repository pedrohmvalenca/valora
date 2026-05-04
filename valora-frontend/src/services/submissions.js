import { api } from "@/services/api";

/**
 * Wrappers do contrato /submissions (Story 4.5 consolidada γ).
 *
 * Endpoints backend:
 * - GET    /api/v1/submissions          (lista paginada, filtros opcionais status + courseId)
 * - GET    /api/v1/submissions/{id}     (detalhe com saldo)
 * - POST   /api/v1/submissions/{id}/approve
 * - POST   /api/v1/submissions/{id}/reject  body: { reason }
 *
 * Erros conhecidos (caller inspeciona error.response?.data?.code):
 * - "BIZ_004" CATEGORY_HOURS_LIMIT_EXCEEDED — saldo insuficiente
 * - "BIZ_007" SUBMISSION_STATUS_IMMUTABLE  — submissão já decidida
 * - "VAL_001" VALIDATION_ERROR             — motivo < 20 chars
 */

export async function listSubmissions({ status, courseId, page = 0, size = 20 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (courseId) params.courseId = courseId;
  const { data } = await api.get("/submissions", { params });
  return data;
}

export async function getSubmissionDetail(id) {
  const { data } = await api.get(`/submissions/${id}`);
  return data;
}

export async function approveSubmission(id, recognizedHours = null) {
  // Aprovação parcial (Story 4.5 nível C): se passar recognizedHours, aprova
  // com horas ajustadas. Sem argumento, aprova com requested (caminho normal).
  // IMPORTANTE: passar `undefined` (não `null`) quando sem body — axios.post
  // com body null envia "null" literal e Spring rejeita; undefined omite body.
  if (recognizedHours != null) {
    await api.post(`/submissions/${id}/approve`, { recognizedHours });
  } else {
    await api.post(`/submissions/${id}/approve`);
  }
}

export async function rejectSubmission(id, reason) {
  await api.post(`/submissions/${id}/reject`, { reason });
}

/**
 * Reverte decisão (APPROVED/REJECTED) → volta a PENDING. Admin only.
 * MODO DEMO — viola RN-0009 (status imutável após decisão) em produção.
 */
export async function revertSubmission(id) {
  await api.post(`/submissions/${id}/revert`);
}
