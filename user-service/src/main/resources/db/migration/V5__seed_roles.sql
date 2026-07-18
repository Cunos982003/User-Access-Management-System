-- V5__seed_roles.sql
-- Insert default roles and authorities (only if not already exist)

INSERT INTO roles (name, description)
SELECT 'ROLE_USER', 'Standard user with basic permissions'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER');

INSERT INTO roles (name, description)
SELECT 'ROLE_MODERATOR', 'Moderator with user management permissions'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MODERATOR');

INSERT INTO roles (name, description)
SELECT 'ROLE_ADMIN', 'Administrator with full system access'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

-- Insert authorities (only if not already exist)
INSERT INTO authorities (name)
SELECT 'READ_USER'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'READ_USER');

INSERT INTO authorities (name)
SELECT 'UPDATE_USER'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'UPDATE_USER');

INSERT INTO authorities (name)
SELECT 'DELETE_USER'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'DELETE_USER');

INSERT INTO authorities (name)
SELECT 'LOCK_USER'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'LOCK_USER');

INSERT INTO authorities (name)
SELECT 'UNLOCK_USER'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'UNLOCK_USER');

INSERT INTO authorities (name)
SELECT 'EXPORT_AUDIT'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'EXPORT_AUDIT');

INSERT INTO authorities (name)
SELECT 'VIEW_AUDIT'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'VIEW_AUDIT');

INSERT INTO authorities (name)
SELECT 'MANAGE_ROLES'
WHERE NOT EXISTS (SELECT 1 FROM authorities WHERE name = 'MANAGE_ROLES');

-- Assign READ_USER to ROLE_USER
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_USER' AND a.name = 'READ_USER'
  AND NOT EXISTS (
    SELECT 1 FROM role_authorities ra
    WHERE ra.role_id = r.id AND ra.authority_id = a.id
  );

-- Assign authorities to ROLE_MODERATOR
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_MODERATOR'
  AND a.name IN ('READ_USER', 'LOCK_USER', 'UNLOCK_USER', 'VIEW_AUDIT')
  AND NOT EXISTS (
    SELECT 1 FROM role_authorities ra
    WHERE ra.role_id = r.id AND ra.authority_id = a.id
  );

-- Assign all authorities to ROLE_ADMIN
INSERT INTO role_authorities (role_id, authority_id)
SELECT r.id, a.id
FROM roles r, authorities a
WHERE r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM role_authorities ra
    WHERE ra.role_id = r.id AND ra.authority_id = a.id
  );