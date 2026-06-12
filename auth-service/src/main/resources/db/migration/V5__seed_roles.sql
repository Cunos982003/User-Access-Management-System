-- V5__seed_roles.sql
-- Insert default roles and authorities

-- Insert roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standard user with basic permissions'),
    ('ROLE_MODERATOR', 'Moderator with user management permissions'),
    ('ROLE_ADMIN', 'Administrator with full system access');

-- Insert authorities
INSERT INTO authorities (name) VALUES
    ('READ_USER'),
    ('UPDATE_USER'),
    ('DELETE_USER'),
    ('LOCK_USER'),
    ('UNLOCK_USER'),
    ('EXPORT_AUDIT'),
    ('VIEW_AUDIT'),
    ('MANAGE_ROLES');

-- Assign authorities to ROLE_USER
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_USER'
  AND a.name IN ('READ_USER');

-- Assign authorities to ROLE_MODERATOR
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_MODERATOR'
  AND a.name IN ('READ_USER', 'LOCK_USER', 'UNLOCK_USER', 'VIEW_AUDIT');

-- Assign all authorities to ROLE_ADMIN
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_ADMIN';
