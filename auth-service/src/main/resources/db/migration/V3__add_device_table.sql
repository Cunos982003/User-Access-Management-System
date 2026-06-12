-- V3__add_device_table.sql
-- Create device tracking table

CREATE TABLE devices (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(200),
    ip_address  VARCHAR(45),
    os          VARCHAR(100),
    browser     VARCHAR(100),
    last_seen   TIMESTAMP   NOT NULL DEFAULT NOW(),
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_devices_user_id ON devices(user_id);
CREATE INDEX idx_devices_is_active ON devices(is_active);
CREATE INDEX idx_devices_last_seen ON devices(last_seen);
