CREATE TABLE companies (
    id SERIAL PRIMARY KEY,
    version INT,
    org_number bytea UNIQUE,
    company_name bytea,
    authorized_signatory bytea,
    search_key_org_number bytea UNIQUE
);

CREATE TABLE case_workers (
    id SERIAL PRIMARY KEY,
    version INT,
    name bytea,
    email bytea UNIQUE,
    password_hash VARCHAR(60),
    search_key_email bytea UNIQUE
);

CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name TEXT UNIQUE
);

CREATE TABLE case_worker_roles(
    worker_id SERIAL,
    role_id SERIAL,

    PRIMARY KEY (worker_id,role_id),

    CONSTRAINT fk_case_worker_roles_worker
        FOREIGN KEY (worker_id)
        REFERENCES case_workers(id),

    CONSTRAINT fk_case_worker_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)

);



CREATE TABLE applications (
    id SERIAL PRIMARY KEY,
    version INT,
    company_id INT REFERENCES companies(id),
    requested_amount DECIMAL(15,2),
    purpose TEXT,
    status VARCHAR(30) DEFAULT 'PENDING_DOCS', -- PENDING_DOCS, UNDER_REVIEW, APPROVED, REJECTED
    decision VARCHAR(20),
    decision_reason TEXT,
    scoring_result TEXT,
    duration_months INT,
    contact_name bytea,
    contact_number bytea,
    contact_email bytea,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    comment TEXT
);

CREATE TABLE documents (
    id SERIAL PRIMARY KEY,
    application_id INT REFERENCES applications(id),
    filename VARCHAR(255),
    doc_type VARCHAR(50),
    uploaded_at TIMESTAMPTZ DEFAULT NOW()
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

-- Roles within our application
INSERT INTO roles (id, name)
VALUES
    (1, 'ROLE_CERTIFIED'),
    (2, 'ROLE_ADMIN');

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