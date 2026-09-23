CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    org_number VARCHAR(20) UNIQUE,
    company_name VARCHAR(200),
    authorized_signatory VARCHAR(100)
);

CREATE TABLE case_workers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100) UNIQUE,
    password_md5 VARCHAR(32)
);

CREATE TABLE applications (
    id SERIAL PRIMARY KEY,
    company_id INT REFERENCES companies(id),
    requested_amount DECIMAL(15,2),
    purpose TEXT,
    status VARCHAR(30) DEFAULT 'PENDING_DOCS', -- PENDING_DOCS, UNDER_REVIEW, APPROVED, REJECTED
    decision VARCHAR(20),
    decision_reason TEXT,
    scoring_result TEXT,
    audit_log TEXT DEFAULT '[]',  -- JSON blob, no separate table
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    contact_name TEXT,
    contact_number TEXT,
    contact_email TEXT
);

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    application_id INT REFERENCES applications(id),
    filename VARCHAR(255),
    doc_type VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE branches (
    id BIGSERIAL PRIMARY KEY,
    branch_name VARCHAR(50),
    bransch_faktor DOUBLE PRECISION,
    bransch_snitts_soliditet DOUBLE PRECISION,
    bransch_snitt_skuldsattning DOUBLE PRECISION,
    bransch_snitt_marginal DOUBLE PRECISION
);

CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY,
    application_id INT REFERENCES applications(id),
    sequence_number BIGINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    action VARCHAR(50) NOT NULL,
    actor VARCHAR(100) NOT NULL,
    data JSONB,
    previous_hash VARCHAR(64),
    current_hash VARCHAR(64),
    schema_version INT NOT NULL,
    signing_key_id VARCHAR(64),
    signature VARCHAR(512),
    CONSTRAINT uq_audit_application_sequence UNIQUE (application_id, sequence_number)
);

CREATE INDEX idx_audit_application_id ON audit_events(application_id);
CREATE INDEX idx_audit_action ON audit_events(action);

-- Seed: two companies (matching BankID mock org numbers)
INSERT INTO companies (org_number, company_name, authorized_signatory) VALUES
('556000-1234', 'Malmö Fastigheter AB', 'Anders Karlsson'),
('556000-5678', 'Göteborg Handel AB', 'Maria Svensson');

-- Case worker (password = "password123")
INSERT INTO case_workers (name, email, password_md5) VALUES
('Karin Handläggare', 'karin@resurs.se', '482c811da5d5b4bc6d497ffa98491e38');

-- Pre-existing application in REVIEW
INSERT INTO applications (company_id, requested_amount, purpose, status, decision, scoring_result, audit_log) VALUES
(1, 500000.00, 'Expansion av verksamheten', 'UNDER_REVIEW', null, 'FLAGGED: soliditet=0.28 (OK), likviditetsgrad=0.95 (FLAGGED), skuldsättningsgrad=2.1 (OK)', '[{"ts":"2026-01-15T10:00:00","action":"APPLICATION_CREATED"},{"ts":"2026-01-15T10:00:01","action":"SCORING_RUN","result":"REVIEW"}]','Anders Karlsson','0702222222','Anka@gmail.bygg');

-- Branch Specific medians for use
INSERT INTO branches (
    branch_name,
    bransch_faktor,
    bransch_snitts_soliditet,
    bransch_snitt_skuldsattning,
    bransch_snitt_marginal
) VALUES
      ('BYGG',         0.85, 0.22, 2.8, 0.04),
      ('HANDEL',       1.10, 0.28, 1.9, 0.03),
      ('IT',           1.20, 0.45, 0.8, 0.15),
      ('FASTIGHET',    0.90, 0.18, 3.5, 0.12),
      ('TILLVERKNING', 0.95, 0.30, 1.5, 0.06),
      ('TRANSPORT',    0.88, 0.20, 2.2, 0.03),
      ('RESTAURANG',   0.80, 0.15, 2.5, 0.05),
      ('FINANS',       1.15, 0.35, 1.2, 0.18),
      ('VÅRD',         1.05, 0.38, 0.9, 0.07),
      ('UTBILDNING',   1.00, 0.32, 1.1, 0.08);