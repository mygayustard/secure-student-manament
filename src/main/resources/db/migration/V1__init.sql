-- V1: initial schema for Secure Student Management API (PostgreSQL)

CREATE TABLE users (
    id          BIGSERIAL    PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(255) NOT NULL,
    password    VARCHAR(100) NOT NULL,          -- BCrypt hash, never plain text
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_users_username_lower ON users (lower(username));
CREATE UNIQUE INDEX uq_users_email_lower    ON users (lower(email));

CREATE TABLE user_roles (
    user_id BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role    VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'STUDENT')),
    PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_tokens (
    id          BIGSERIAL    PRIMARY KEY,
    token_hash  VARCHAR(64)  NOT NULL UNIQUE,   -- SHA-256 of the opaque token; raw token is never stored
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);

CREATE TABLE students (
    id                   BIGSERIAL    PRIMARY KEY,
    registration_number  VARCHAR(50)  NOT NULL UNIQUE,
    full_name            VARCHAR(150) NOT NULL,
    email                VARCHAR(255) NOT NULL UNIQUE,
    phone                VARCHAR(30),
    gender               VARCHAR(10),
    date_of_birth        DATE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);
