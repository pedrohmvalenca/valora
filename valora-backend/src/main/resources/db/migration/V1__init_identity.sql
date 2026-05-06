CREATE EXTENSION IF NOT EXISTS pgcrypto;

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

CREATE TABLE coordinator_course (
    coordinator_id  UUID NOT NULL,
    course_id       UUID NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (coordinator_id, course_id),
    CONSTRAINT fk_coordinator_course_user   FOREIGN KEY (coordinator_id) REFERENCES users(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_coordinator_course_course FOREIGN KEY (course_id)      REFERENCES courses(id) ON DELETE RESTRICT,
    CONSTRAINT uk_coordinator_course UNIQUE (coordinator_id, course_id)
);

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

INSERT INTO users (id, email, password_hash, name, profile, is_active)
VALUES (
    gen_random_uuid(),
    'admin@valora.local',
    '$2b$12$9qXNdy6QXExGNlC0Gkecoua59XBL6XrKAr8aFdOnGijDqNBjp28K2',
    'Administrador VALORA (dev)',
    'ADMINISTRATOR',
    TRUE
);
