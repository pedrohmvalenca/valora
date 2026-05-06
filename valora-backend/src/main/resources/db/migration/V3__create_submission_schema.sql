CREATE TABLE submissions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id          UUID NOT NULL,
    course_id           UUID NOT NULL,
    category_id         UUID NOT NULL,
    description         TEXT NOT NULL,
    requested_hours     INTEGER NOT NULL,
    proof_path          VARCHAR(500),                                -- placeholder Entrega 1; Entrega 2 implementa upload real
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by          UUID,
    decided_at          TIMESTAMP WITH TIME ZONE,
    rejection_reason    TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_submissions_student   FOREIGN KEY (student_id)  REFERENCES students(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_course    FOREIGN KEY (course_id)   REFERENCES courses(id)    ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_category  FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_decider   FOREIGN KEY (decided_by)  REFERENCES users(id)      ON DELETE RESTRICT,
    CONSTRAINT ck_submissions_status    CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT ck_submissions_hours_positive CHECK (requested_hours > 0),
    CONSTRAINT ck_submissions_rejection_reason_or_null CHECK (
        (status = 'REJECTED' AND rejection_reason IS NOT NULL AND char_length(rejection_reason) >= 20) OR
        (status <> 'REJECTED')
    ),
    CONSTRAINT ck_submissions_decided_consistency CHECK (
        (status = 'PENDING'  AND decided_by IS NULL AND decided_at IS NULL) OR
        (status <> 'PENDING' AND decided_by IS NOT NULL AND decided_at IS NOT NULL)
    )
);

CREATE INDEX idx_submissions_balance
    ON submissions(student_id, course_id, category_id, status);

CREATE INDEX idx_submissions_course_created
    ON submissions(course_id, created_at DESC);
