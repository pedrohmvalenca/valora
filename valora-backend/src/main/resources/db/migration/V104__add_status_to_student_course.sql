-- V104__add_status_to_student_course.sql
-- Story 3.10 — Status do vínculo student_course (CURSANDO/CONCLUIDO/ABANDONADO).
--
-- Numerada V104 (não V4 como na spec) para respeitar convenção do repo:
-- V100..V103 já são seeds aplicados em local; nova migration de schema
-- entra como a próxima após V103 para evitar exigir flyway.out-of-order=true.
-- Mesma decisão tomada na V101 (ver cabeçalho dela). Comportamento idêntico
-- em homol/prod (que não carregam seeds): V104 é a próxima migration de schema.
--
-- Backfill implícito: DEFAULT 'CURSANDO' cobre todas as linhas existentes
-- (vínculos criados pelos seeds e pelas Stories 3.2/3.9). Sem acentos no enum
-- SQL — evita surpresa de encoding em ambientes ISO-8859-1.
--
-- Índice composto (course_id, status) antecipa filtros da Story 3.11
-- ("alunos cursando em <curso>"). Custo desprezível no MVP.

ALTER TABLE student_course
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CURSANDO',
    ADD CONSTRAINT ck_student_course_status
        CHECK (status IN ('CURSANDO', 'CONCLUIDO', 'ABANDONADO'));

CREATE INDEX idx_student_course_course_status
    ON student_course(course_id, status);
