-- V101__add_recognized_hours_to_submission.sql
-- Story 4.5 nível C (Tier A do batch γ — 2026-05-04). Renomeada de V4 para
-- V101 (Flyway: out-of-order vs V100 já aplicada).
--
-- Aprovação parcial (RN-0004 + EXT-02): coordenador pode aprovar com
-- horas ajustadas (≤ saldo restante).
--
-- Ordem importa: 1) ADD COLUMN, 2) BACKFILL, 3) ADD CONSTRAINT.
-- Sem o backfill antes do CHECK, linhas APPROVED do seed V100 violariam.

-- 1) Add column nullable
ALTER TABLE submissions
    ADD COLUMN recognized_hours INTEGER;

-- 2) Backfill das submissões já APPROVED (recognized = requested)
UPDATE submissions
   SET recognized_hours = requested_hours
 WHERE status = 'APPROVED';

-- 3) Constraint: se aprovado → obrigatório, positivo, ≤ requested
ALTER TABLE submissions
    ADD CONSTRAINT ck_submissions_recognized_hours_consistency CHECK (
        (status <> 'APPROVED' AND recognized_hours IS NULL) OR
        (status = 'APPROVED' AND recognized_hours IS NOT NULL
            AND recognized_hours > 0
            AND recognized_hours <= requested_hours)
    );

-- 4) Atualizar index composto (inclui recognized_hours para acelerar SUM)
DROP INDEX IF EXISTS idx_submissions_balance;
CREATE INDEX idx_submissions_balance
    ON submissions(student_id, course_id, category_id, status)
    INCLUDE (recognized_hours);
