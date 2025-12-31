-- Indexes for OLTP and analytics schema.
-- Guard each CREATE INDEX with a to_regclass() check so this script
-- is safe to run before JPA has created the tables.

DO $$
BEGIN
    IF to_regclass('app.appointment') IS NOT NULL THEN
        -- Speed up lookups by tenant and time
        CREATE INDEX IF NOT EXISTS idx_appointment_org_clinic_scheduled
            ON app.appointment (organization_id, clinic_id, scheduled_at);

        -- Common filter: tenant + status
        CREATE INDEX IF NOT EXISTS idx_appointment_org_clinic_status
            ON app.appointment (organization_id, clinic_id, status);
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('app.outbox_event') IS NOT NULL THEN
        -- Efficient querying by processed_at and id (useful for housekeeping
        -- or alternate poll strategies)
        CREATE INDEX IF NOT EXISTS idx_outbox_event_unprocessed
            ON app.outbox_event (processed_at, id);
    END IF;
END$$;

DO $$
BEGIN
    IF to_regclass('analytics.appointment_funnel_fact') IS NOT NULL THEN
        -- Funnel reads by org+clinic
        CREATE INDEX IF NOT EXISTS idx_appointment_funnel_org_clinic
            ON analytics.appointment_funnel_fact (organization_id, clinic_id);
    END IF;
END$$;
