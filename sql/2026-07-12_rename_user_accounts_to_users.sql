-- Rename the user aggregate table to `users` so every layer (URL, package,
-- controller, service, entity, repository, table) uses the same `user` name.
-- Apply after 2026-07-12_add_user_accounts_updated_at.sql, before deploying code
-- that runs with spring.jpa.hibernate.ddl-auto=validate.

RENAME TABLE user_accounts TO users;

ALTER TABLE users RENAME INDEX uk_user_accounts_provider_user TO uk_users_provider_user;
ALTER TABLE users RENAME INDEX uk_user_accounts_auth_subject TO uk_users_auth_subject;

-- MySQL automatically retargets foreign keys on the collection tables
-- (user_disability_types, user_interests) to the renamed `users` table.
