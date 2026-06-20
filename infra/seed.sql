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
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    application_id INT REFERENCES applications(id),
    filename VARCHAR(255),
    doc_type VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT NOW()
);

-- Seed: two companies (matching BankID mock org numbers)
INSERT INTO companies (org_number, company_name, authorized_signatory) VALUES
('556000-1234', 'Malmö Fastigheter AB', 'Anders Karlsson'),
('556000-5678', 'Göteborg Handel AB', 'Maria Svensson');

-- Case worker (password = "password123")
INSERT INTO case_workers (name, email, password_md5) VALUES
('Karin Handläggare', 'karin@resurs.se', '482c811da5d5b4bc6d497ffa98491e38');

-- Pre-existing application in REVIEW
INSERT INTO applications (company_id, requested_amount, purpose, status, decision, scoring_result, audit_log) VALUES
(1, 500000.00, 'Expansion av verksamheten', 'UNDER_REVIEW', null, 'FLAGGED: soliditet=0.28 (OK), likviditetsgrad=0.95 (FLAGGED), skuldsättningsgrad=2.1 (OK)', '[{"ts":"2026-01-15T10:00:00","action":"APPLICATION_CREATED"},{"ts":"2026-01-15T10:00:01","action":"SCORING_RUN","result":"REVIEW"}]');
