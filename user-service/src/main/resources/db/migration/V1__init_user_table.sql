-- V1__init_user_table.sql
-- Create core user management tables

-- users table
CREATE TABLE IF NOT EXISTS users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(50)  UNIQUE NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100),
    phone         VARCHAR(20),
    avatar_url    VARCHAR(500),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login    TIMESTAMP
);

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);

-- roles table
CREATE TABLE IF NOT EXISTS roles (
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(200)
);

-- authorities table (fine-grained permissions)
CREATE TABLE IF NOT EXISTS authorities (
    id   SERIAL PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL
);

-- user_roles (many-to-many)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID    NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- role_authorities (many-to-many)
CREATE TABLE IF NOT EXISTS role_authorities (
    role_id      INTEGER NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    authority_id INTEGER NOT NULL REFERENCES authorities(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, authority_id)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_role_authorities_role_id ON role_authorities(role_id);
CREATE INDEX IF NOT EXISTS idx_role_authorities_authority_id ON role_authorities(authority_id);