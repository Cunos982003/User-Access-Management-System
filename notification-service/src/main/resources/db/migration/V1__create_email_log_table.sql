-- V1__create_email_log_table.sql
-- Create email log table for notification service

CREATE TABLE IF NOT EXISTS email_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient       VARCHAR(255) NOT NULL,
    subject         VARCHAR(500) NOT NULL,
    body            TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL,
    error_message   VARCHAR(1000),
    sent_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- Create indexes for common queries
CREATE INDEX IF NOT EXISTS idx_email_recipient ON email_logs(recipient);
CREATE INDEX IF NOT EXISTS idx_email_status ON email_logs(status);
CREATE INDEX IF NOT EXISTS idx_email_sent_at ON email_logs(sent_at);
CREATE INDEX IF NOT EXISTS idx_email_created_at ON email_logs(created_at);

-- Add comments
COMMENT ON TABLE email_logs IS 'Email notification log for tracking all sent emails';
COMMENT ON COLUMN email_logs.recipient IS 'Email address of the recipient';
COMMENT ON COLUMN email_logs.status IS 'Status of the email (PENDING, SENT, FAILED)';
COMMENT ON COLUMN email_logs.sent_at IS 'Timestamp when email was successfully sent';
