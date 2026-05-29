-- VR Domain Tables
-- Run first — 002_ruoyi_rbac.sql depends on tenants(id) via sys_user_tenant
-- ============================================================

-- 1. Tenants (VR space operating companies)
CREATE TABLE IF NOT EXISTS tenants (
    id           BIGSERIAL    PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    contact_info TEXT,
    created_at   TIMESTAMP  NOT NULL DEFAULT NOW()
);

-- 2. Venues (physical VR sites)
CREATE TABLE IF NOT EXISTS venues (
    id               BIGSERIAL    PRIMARY KEY,
    tenant_id        BIGINT       NOT NULL REFERENCES tenants(id),
    name             VARCHAR(255) NOT NULL,
    address          TEXT,
    controller_token VARCHAR(255),
    created_at       TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_venues_tenant_id ON venues(tenant_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_venues_token ON venues(controller_token)
    WHERE controller_token IS NOT NULL;

-- 3. Applications (VR software titles)
CREATE TABLE IF NOT EXISTS applications (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP  NOT NULL DEFAULT NOW()
);

-- 4. Application versions
CREATE TABLE IF NOT EXISTS application_versions (
    id             BIGSERIAL    PRIMARY KEY,
    application_id BIGINT       NOT NULL REFERENCES applications(id),
    version        VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_versions_app_id ON application_versions(application_id);

-- 5. Licenses (tenant+app authorization with optional quota)
CREATE TABLE IF NOT EXISTS licenses (
    id             BIGSERIAL   PRIMARY KEY,
    tenant_id      BIGINT      NOT NULL REFERENCES tenants(id),
    application_id BIGINT      NOT NULL REFERENCES applications(id),
    granted        BOOLEAN     NOT NULL DEFAULT FALSE,
    quota_type     VARCHAR(50),
    quota_limit    BIGINT,
    quota_used     BIGINT      DEFAULT 0,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, application_id)
);

CREATE INDEX IF NOT EXISTS idx_licenses_tenant_id ON licenses(tenant_id);

-- 6. Sessions (active/completed VR usage tracked per venue+app)
CREATE TABLE IF NOT EXISTS sessions (
    id             BIGSERIAL    PRIMARY KEY,
    venue_id       BIGINT       NOT NULL REFERENCES venues(id),
    application_id BIGINT       NOT NULL REFERENCES applications(id),
    version        VARCHAR(100) NOT NULL,
    started_at     TIMESTAMP  NOT NULL,
    ended_at       TIMESTAMP,
    status         VARCHAR(20)  NOT NULL DEFAULT 'active',
    created_at     TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_session_status CHECK (status IN ('active', 'normal', 'abnormal'))
);

CREATE INDEX IF NOT EXISTS idx_sessions_venue_id ON sessions(venue_id);
CREATE INDEX IF NOT EXISTS idx_sessions_app_id    ON sessions(application_id);
CREATE INDEX IF NOT EXISTS idx_sessions_status    ON sessions(status);
CREATE INDEX IF NOT EXISTS idx_sessions_started   ON sessions(started_at);
