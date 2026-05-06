CREATE TABLE students (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    registration_code   VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    email               VARCHAR(255) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_students_registration ON students(registration_code);
CREATE INDEX idx_students_email ON students(email);

CREATE TABLE student_course (
    student_id  UUID NOT NULL,
    course_id   UUID NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (student_id, course_id),
    CONSTRAINT fk_student_course_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE RESTRICT,
    CONSTRAINT fk_student_course_course  FOREIGN KEY (course_id)  REFERENCES courses(id)  ON DELETE RESTRICT
);

CREATE TABLE categories (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    group_type   VARCHAR(20)  NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_categories_group_type CHECK (group_type IN ('TEACHING','RESEARCH','EXTENSION'))
);

CREATE INDEX idx_categories_group ON categories(group_type);

CREATE TABLE category_course (
    category_id  UUID NOT NULL,
    course_id    UUID NOT NULL,
    max_hours    INTEGER NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    PRIMARY KEY (category_id, course_id),
    CONSTRAINT fk_category_course_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_category_course_course   FOREIGN KEY (course_id)   REFERENCES courses(id)   ON DELETE RESTRICT,
    CONSTRAINT ck_category_course_max_hours CHECK (max_hours > 0)
);
