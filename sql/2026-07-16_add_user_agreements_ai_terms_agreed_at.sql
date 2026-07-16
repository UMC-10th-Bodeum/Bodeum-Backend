-- Store the exact UTC instant when the optional AI chatbot terms were agreed.
-- Apply after 2026-07-15_extract_user_agreements.sql, before deploying code
-- that runs with spring.jpa.hibernate.ddl-auto=validate.

SET @ai_terms_agreed_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_agreements'
      AND COLUMN_NAME = 'ai_terms_agreed_at'
);

SET @sql := IF(
    @ai_terms_agreed_at_exists = 0,
    'ALTER TABLE user_agreements ADD COLUMN ai_terms_agreed_at TIMESTAMP NULL AFTER ai_terms_agreed',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE user_agreements
SET ai_terms_agreed_at = agreed_at
WHERE ai_terms_agreed = TRUE
  AND ai_terms_agreed_at IS NULL;
