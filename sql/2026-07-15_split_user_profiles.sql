-- Split profile data out of users so the physical schema matches the ERD:
-- users -> child_profiles -> child_disabilities
-- users -> guardian_profiles
-- users -> user_interests
--
-- Apply after 2026-07-13_add_region_master_and_user_region_fk.sql, before
-- deploying code that runs with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE child_profiles (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    nickname     VARCHAR(20)  NULL,
    birth        VARCHAR(7)   NULL,
    keyword_text VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_child_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_child_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO child_profiles (user_id, nickname, birth, keyword_text)
SELECT id, child_nickname, child_birth, keyword_text
FROM users
WHERE child_nickname IS NOT NULL
   OR child_birth IS NOT NULL
   OR keyword_text IS NOT NULL
   OR EXISTS (
       SELECT 1
       FROM user_disability_types udt
       WHERE udt.user_id = users.id
   );

CREATE TABLE child_disabilities (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    child_profile_id   BIGINT NOT NULL,
    disability_type    VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_child_disabilities_child_profile
        FOREIGN KEY (child_profile_id) REFERENCES child_profiles (id)
);

INSERT INTO child_disabilities (child_profile_id, disability_type)
SELECT
    cp.id,
    CASE udt.disability_type_id
        WHEN 1 THEN 'AUTISM'
        WHEN 2 THEN 'INTELLECTUAL_DISABILITY'
        WHEN 3 THEN 'CEREBRAL_PALSY'
        WHEN 4 THEN 'ADHD'
        WHEN 5 THEN 'DEVELOPMENTAL_DELAY'
        WHEN 6 THEN 'LANGUAGE_DISORDER'
        WHEN 7 THEN 'ETC'
        ELSE 'ETC'
    END
FROM user_disability_types udt
JOIN child_profiles cp ON cp.user_id = udt.user_id;

CREATE TABLE guardian_profiles (
    id                  BIGINT      NOT NULL AUTO_INCREMENT,
    user_id             BIGINT      NOT NULL,
    nickname            VARCHAR(20) NULL,
    region_id           BIGINT      NULL,
    guardian_type       VARCHAR(50) NULL,
    community_role_type VARCHAR(50) NULL,
    point               INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_guardian_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_guardian_profiles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_guardian_profiles_region FOREIGN KEY (region_id) REFERENCES regions (id)
);

INSERT INTO guardian_profiles (user_id, nickname, region_id, guardian_type, community_role_type, point)
SELECT id, guardian_nickname, region_id, guardian_type, community_role_type, point
FROM users
WHERE guardian_nickname IS NOT NULL
   OR region_id IS NOT NULL
   OR guardian_type IS NOT NULL
   OR community_role_type IS NOT NULL
   OR point <> 0;

CREATE TABLE user_interests_new (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    user_id           BIGINT      NOT NULL,
    interest_category VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_interests_user FOREIGN KEY (user_id) REFERENCES users (id)
);

INSERT INTO user_interests_new (user_id, interest_category)
SELECT
    user_id,
    CASE interest_category_id
        WHEN 1 THEN 'INSTITUTION'
        WHEN 2 THEN 'HOSPITAL'
        WHEN 3 THEN 'WELFARE'
        WHEN 4 THEN 'EMPLOYMENT'
        WHEN 5 THEN 'EDUCATION'
        ELSE 'INSTITUTION'
    END
FROM user_interests;

DROP TABLE user_interests;
RENAME TABLE user_interests_new TO user_interests;

DROP TABLE user_disability_types;

ALTER TABLE users
    DROP COLUMN child_nickname,
    DROP COLUMN child_birth,
    DROP COLUMN keyword_text,
    DROP COLUMN region_id,
    DROP COLUMN guardian_nickname,
    DROP COLUMN guardian_type,
    DROP COLUMN community_role_type,
    DROP COLUMN point;
