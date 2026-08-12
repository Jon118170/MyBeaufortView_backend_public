-- ============================================================
-- DEV-ONLY SEED
--
-- This migration seeds a deterministic admin account
-- for LOCAL DEVELOPMENT and SWAGGER testing ONLY.
--
-- Login:
--   email:    admin@local.dev
--   password: change-me
--
-- The password below is a BCrypt hash of "change-me".
-- DO NOT copy this pattern to production migrations.
-- ============================================================

INSERT INTO users (username, name, email, password, role)
VALUES (
  'admin',
  'Dev Admin',
  'admin@local.dev',
  '$2a$10$Hc4EOEnWY45TgvmXwtAzE.rRKNdM12XqoYktLZ7fB.VDYDWl0Z7/m',
  'ADMIN'
)
ON CONFLICT (email) DO UPDATE
SET password = EXCLUDED.password,
    role = EXCLUDED.role,
    username = EXCLUDED.username,
    name = EXCLUDED.name;
