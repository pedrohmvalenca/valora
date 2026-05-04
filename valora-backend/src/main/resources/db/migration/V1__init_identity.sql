-- V1__init_identity.sql
-- Story 1.1 — Backend Skeleton + Migration V1 (Identity Schema)
--
-- Atualizado em 2026-05-03 conforme ADR-0013 (convergência com cldavi/valoraapi)
-- e ADR-0014 (naming código em inglês com comentários em PT-BR).
--
-- Tabelas: users, courses, coordinator_course, audit_log
-- + seed Administrator para dev local (admin@valora.local / Admin@123)
--
-- ATENÇÃO: o seed Administrator é APENAS para dev local. NÃO usar Admin@123 em homol/prod.
-- O hash abaixo foi gerado via BCrypt cost 12 e valida exatamente para "Admin@123".

-- ============================================================
-- Extensão obrigatória para gen_random_uuid()
-- ============================================================
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================
-- Tabela: users
--   - profile: ADMINISTRATOR | COORDINATOR | STUDENT (CHECK)
--   - password_hash: BCrypt cost 12 (RNF-0003)
--   - timestamps em UTC (TIMESTAMP WITH TIME ZONE)
-- ============================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    profile         VARCHAR(20)  NOT NULL,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_users_profile CHECK (profile IN ('ADMINISTRATOR', 'COORDINATOR', 'STUDENT'))
);

CREATE INDEX idx_users_email ON users(email);

-- ============================================================
-- Tabela: courses
-- ============================================================
CREATE TABLE courses (
    id                          UUID PRIMARY KEY,
    name                        VARCHAR(255) NOT NULL,
    code                        VARCHAR(20)  NOT NULL UNIQUE,
    minimum_workload_hours      INTEGER      NOT NULL DEFAULT 100,
    is_active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_courses_workload_positive CHECK (minimum_workload_hours > 0)
);

CREATE INDEX idx_courses_code ON courses(code);

-- ============================================================
-- Tabela: coordinator_course (junction N:N — D4)
--   - PK composta + UNIQUE redundante (defesa em profundidade)
--   - FKs com ON DELETE RESTRICT (RN-0008 / ADR-0005)
-- ============================================================
CREATE TABLE coordinator_course (
    coordinator_id  UUID NOT NULL,
    course_id       UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (coordinator_id, course_id),
    CONSTRAINT fk_coordinator_course_user   FOREIGN KEY (coordinator_id) REFERENCES users(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_coordinator_course_course FOREIGN KEY (course_id)      REFERENCES courses(id) ON DELETE RESTRICT,
    CONSTRAINT uk_coordinator_course UNIQUE (coordinator_id, course_id)
);

-- ============================================================
-- Tabela: audit_log (audit — RF-0032/0033 + EXT-06)
--   - course_id antecipado em V1 (EXT-06 — evita ALTER TABLE depois)
--   - entity_type nullable (LOGIN/LOGOUT não têm entity)
--   - payload_json JSONB para delta antes/depois
-- ============================================================
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL,
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(30),
    entity_id       UUID,
    course_id       UUID,
    payload_json    JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_audit_log_user    FOREIGN KEY (user_id)   REFERENCES users(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_audit_log_course  FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE RESTRICT,
    CONSTRAINT ck_audit_log_entity_type CHECK (
        entity_type IS NULL OR
        entity_type IN ('USER', 'COURSE', 'COORDINATOR', 'STUDENT', 'CATEGORY', 'SUBMISSION')
    )
);

CREATE INDEX idx_audit_log_user_created   ON audit_log(user_id, created_at DESC);
CREATE INDEX idx_audit_log_course_created ON audit_log(course_id, created_at DESC) WHERE course_id IS NOT NULL;

-- ============================================================
-- Seed: Administrator inicial — APENAS DEV LOCAL
--   senha = Admin@123 (BCrypt cost 12, $2b$ variant — Spring Security aceita)
--   Hash gerado via Python bcrypt em 30/04/2026; valida com checkpw('Admin@123', hash) = True
-- ============================================================
INSERT INTO users (id, email, password_hash, name, profile, is_active)
VALUES (
    gen_random_uuid(),
    'admin@valora.local',
    '$2b$12$9qXNdy6QXExGNlC0Gkecoua59XBL6XrKAr8aFdOnGijDqNBjp28K2',
    'Administrador VALORA (dev)',
    'ADMINISTRATOR',
    TRUE
);
