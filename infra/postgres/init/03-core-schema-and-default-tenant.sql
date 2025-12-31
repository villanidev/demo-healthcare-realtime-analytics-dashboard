-- Create core tenant tables if they don't exist yet and seed a
-- deterministic default tenant for load tests (org=1, clinic=1, patient=1).

-- Organization table
CREATE TABLE IF NOT EXISTS app.organization (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(160) NOT NULL
);

-- Clinic table
CREATE TABLE IF NOT EXISTS app.clinic (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES app.organization(id),
    name             VARCHAR(160) NOT NULL
);

-- Patient account table
CREATE TABLE IF NOT EXISTS app.patient_account (
    id               BIGSERIAL PRIMARY KEY,
    organization_id  BIGINT       NOT NULL REFERENCES app.organization(id),
    clinic_id        BIGINT       NOT NULL REFERENCES app.clinic(id),
    display_name     VARCHAR(160) NOT NULL
);

-- Seed deterministic default tenant rows used by k6 and the dashboard.
INSERT INTO app.organization (id, name)
VALUES (1, 'Demo Health Org')
ON CONFLICT (id) DO NOTHING;

INSERT INTO app.clinic (id, organization_id, name)
VALUES (1, 1, 'Main Clinic')
ON CONFLICT (id) DO NOTHING;

INSERT INTO app.patient_account (id, organization_id, clinic_id, display_name)
VALUES (1, 1, 1, 'Demo Patient')
ON CONFLICT (id) DO NOTHING;
