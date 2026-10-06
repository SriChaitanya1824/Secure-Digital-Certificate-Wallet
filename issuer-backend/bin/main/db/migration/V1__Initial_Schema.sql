CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email ON users(email);

CREATE TABLE issuers (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    public_key TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'TRUSTED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_issuer_status ON issuers(status);

CREATE TABLE certificates (
    id VARCHAR(36) PRIMARY KEY,
    certificate_id VARCHAR(255) NOT NULL UNIQUE,
    credential_type VARCHAR(100) NOT NULL,
    issuer_id VARCHAR(36) NOT NULL,
    subject_id VARCHAR(255) NOT NULL,
    subject_name VARCHAR(255) NOT NULL,
    issued_at BIGINT NOT NULL,
    valid_from BIGINT NOT NULL,
    expires_at BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    signature TEXT NOT NULL,
    proof TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (issuer_id) REFERENCES issuers(id)
);

CREATE INDEX idx_cert_status ON certificates(status);
CREATE INDEX idx_cert_issuer ON certificates(issuer_id);
CREATE INDEX idx_cert_expires ON certificates(expires_at);

CREATE TABLE certificate_claims (
    id VARCHAR(36) PRIMARY KEY,
    certificate_id VARCHAR(36) NOT NULL,
    claim_key VARCHAR(255) NOT NULL,
    claim_value TEXT NOT NULL,
    FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);

CREATE INDEX idx_claim_cert ON certificate_claims(certificate_id);

CREATE TABLE revocations (
    id VARCHAR(36) PRIMARY KEY,
    certificate_id VARCHAR(36) NOT NULL UNIQUE,
    reason VARCHAR(255) NOT NULL,
    revoked_at BIGINT NOT NULL,
    revoked_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);

CREATE INDEX idx_revoke_cert ON revocations(certificate_id);

CREATE TABLE certificate_events (
    id VARCHAR(36) PRIMARY KEY,
    certificate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    timestamp BIGINT NOT NULL,
    details TEXT,
    FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);

CREATE INDEX idx_event_cert ON certificate_events(certificate_id);
CREATE INDEX idx_event_type ON certificate_events(event_type);

CREATE TABLE verification_events (
    id VARCHAR(36) PRIMARY KEY,
    certificate_id VARCHAR(36) NOT NULL,
    result VARCHAR(50) NOT NULL,
    is_offline BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp BIGINT NOT NULL,
    FOREIGN KEY (certificate_id) REFERENCES certificates(id) ON DELETE CASCADE
);

CREATE INDEX idx_verify_cert ON verification_events(certificate_id);
CREATE INDEX idx_verify_time ON verification_events(timestamp);

CREATE TABLE refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_token ON refresh_tokens(user_id);
CREATE INDEX idx_token_expiry ON refresh_tokens(expires_at);

CREATE TABLE audit_logs (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    timestamp BIGINT NOT NULL,
    details TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_time ON audit_logs(timestamp);
