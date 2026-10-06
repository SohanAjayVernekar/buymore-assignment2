-- Contact Us form submissions, stored durably in PostgreSQL.
-- Uses IF NOT EXISTS so it is safe to run on fresh and existing databases.
BEGIN;
CREATE TABLE IF NOT EXISTS contact_submissions (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
COMMIT;
