-- Extract agreement flags out of users into a dedicated 1:1 table so the
-- physical schema matches the ERD (user_agreement entity).
--
-- Apply after 2026-07-15_split_user_profiles.sql, before deploying code that
-- runs with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE user_agreements (
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

INSERT INTO user_agreements (user_id, service_terms_agreed, privacy_policy_agreed, ai_terms_agreed, agreed_at)
SELECT id, service_terms_agreed, privacy_policy_agreed, ai_terms_agreed, agreement_agreed_at
FROM users
WHERE service_terms_agreed = TRUE
   OR privacy_policy_agreed = TRUE
   OR ai_terms_agreed = TRUE
   OR agreement_agreed_at IS NOT NULL;

ALTER TABLE users
    DROP COLUMN service_terms_agreed,
    DROP COLUMN privacy_policy_agreed,
    DROP COLUMN ai_terms_agreed,
    DROP COLUMN agreement_agreed_at;
