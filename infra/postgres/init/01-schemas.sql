CREATE SCHEMA IF NOT EXISTS app;
CREATE SCHEMA IF NOT EXISTS analytics;

-- Optional: keep search_path predictable for manual psql sessions
ALTER ROLE healthcare_app SET search_path TO app, analytics, public;
