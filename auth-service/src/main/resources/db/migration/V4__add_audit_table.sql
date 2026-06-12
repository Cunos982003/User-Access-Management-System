-- V4__add_audit_table.sql
-- Create audit logging table

CREATE TABLE audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID,
    username        VARCHAR(50),
    action          VARCHAR(50) NOT NULL,
    resource_type   VARCHAR(50),
    resource_id     UUID,
    description     VARCHAR(500),
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    status          VARCHAR(20) NOT NULL,
    error_message   VARCHAR(1000),
    timestamp       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_ms     BIGINT,
    request_method  VARCHAR(10),
    request_path    VARCHAR(500),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create indexes for common queries
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX idx_audit_ip ON audit_logs(ip_address);
CREATE INDEX idx_audit_status ON audit_logs(status);

-- Add comments
COMMENT ON TABLE audit_logs IS 'System audit log for tracking all user actions and events';
COMMENT ON COLUMN audit_logs.user_id IS 'User who performed the action (nullable for system events)';
COMMENT ON COLUMN audit_logs.action IS 'Action performed (e.g., USER_LOGIN, USER_REGISTER, PASSWORD_CHANGE)';
COMMENT ON COLUMN audit_logs.resource_type IS 'Type of resource affected (e.g., USER, ROLE, SETTING)';
COMMENT ON COLUMN audit_logs.status IS 'Outcome of the action (SUCCESS or FAILURE)';
COMMENT ON COLUMN audit_logs.duration_ms IS 'Duration of the operation in milliseconds';
