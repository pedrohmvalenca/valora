ALTER TABLE submissions
    ADD COLUMN recognized_hours INTEGER;

UPDATE submissions
   SET recognized_hours = requested_hours
 WHERE status = 'APPROVED';

ALTER TABLE submissions
    ADD CONSTRAINT ck_submissions_recognized_hours_consistency CHECK (
        (status <> 'APPROVED' AND recognized_hours IS NULL) OR
        (status = 'APPROVED' AND recognized_hours IS NOT NULL
            AND recognized_hours > 0
            AND recognized_hours <= requested_hours)
    );

DROP INDEX IF EXISTS idx_submissions_balance;
CREATE INDEX idx_submissions_balance
    ON submissions(student_id, course_id, category_id, status)
    INCLUDE (recognized_hours);
