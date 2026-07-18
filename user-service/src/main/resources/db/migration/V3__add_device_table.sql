-- V3__add_device_table.sql
-- Create devices table for device tracking

CREATE TABLE IF NOT EXISTS devices (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(200),
    ip_address  VARCHAR(45),
    os          VARCHAR(100),
    browser     VARCHAR(100),
    last_seen   TIMESTAMP   NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE
);

CREATE INDEX IF NOT EXISTS idx_devices_user_id ON devices(user_id);
CREATE INDEX IF NOT EXISTS idx_devices_last_seen ON devices(last_seen);