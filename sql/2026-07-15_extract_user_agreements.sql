-- Extract agreement flags out of users into a dedicated 1:1 table so the
-- physical schema matches the ERD (user_agreement entity).
--
-- Apply after 2026-07-15_split_user_profiles.sql, before deploying code that
-- runs with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS user_agreements (
    id                    BIGINT    NOT NULL AUTO_INCREMENT,
    user_id               BIGINT    NOT NULL,
    service_terms_agreed  BOOLEAN   NOT NULL DEFAULT FALSE,
    privacy_policy_agreed BOOLEAN   NOT NULL DEFAULT FALSE,
    ai_terms_agreed       BOOLEAN   NOT NULL DEFAULT FALSE,
    agreed_at             TIMESTAMP NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_agreements_user UNIQUE (user_id),
    CONSTRAINT fk_user_agreements_user FOREIGN KEY (user_id) REFERENCES users (id)
);

SET @has_legacy_agreement_columns := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME IN (
          'service_terms_agreed',
          'privacy_policy_agreed',
          'ai_terms_agreed',
          'agreement_agreed_at'
      )
);

SET @sql := IF(
    @has_legacy_agreement_columns = 4,
    'INSERT INTO user_agreements (user_id, service_terms_agreed, privacy_policy_agreed, ai_terms_agreed, agreed_at)
     SELECT id, service_terms_agreed, privacy_policy_agreed, ai_terms_agreed, agreement_agreed_at
     FROM users u
     WHERE (service_terms_agreed = TRUE
        OR privacy_policy_agreed = TRUE
        OR ai_terms_agreed = TRUE
        OR agreement_agreed_at IS NOT NULL)
       AND NOT EXISTS (
           SELECT 1
           FROM user_agreements ua
           WHERE ua.user_id = u.id
       )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'service_terms_agreed'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN service_terms_agreed', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'privacy_policy_agreed'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN privacy_policy_agreed', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'ai_terms_agreed'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN ai_terms_agreed', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'agreement_agreed_at'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN agreement_agreed_at', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
