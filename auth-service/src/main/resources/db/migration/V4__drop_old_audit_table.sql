-- V4__drop_old_audit_table.sql
-- Drop old audit_logs table if exists (will be recreated by audit-service)

DROP TABLE IF EXISTS audit_logs CASCADE;
