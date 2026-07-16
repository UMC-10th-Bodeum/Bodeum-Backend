-- Align user_accounts with the ERD and UserAccount.updatedAt.
-- Apply once before deploying code that runs with spring.jpa.hibernate.ddl-auto=validate.

ALTER TABLE user_accounts
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

ALTER TABLE user_accounts
    ADD COLUMN updated_at DATETIME(6) NULL
        AFTER created_at;

UPDATE user_accounts
SET updated_at = created_at
WHERE updated_at IS NULL;

ALTER TABLE user_accounts
    MODIFY COLUMN updated_at DATETIME(6) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6);
