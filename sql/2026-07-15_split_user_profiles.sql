-- Split profile data out of users so the physical schema matches the ERD:
-- users -> child_profiles -> child_disabilities
-- users -> guardian_profiles
-- users -> user_interests
--
-- Apply after 2026-07-13_add_region_master_and_user_region_fk.sql, before
-- deploying code that runs with spring.jpa.hibernate.ddl-auto=validate.

CREATE TABLE IF NOT EXISTS child_profiles (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    user_id      BIGINT       NOT NULL,
    nickname     VARCHAR(20)  NULL,
    birth        VARCHAR(7)   NULL,
    keyword_text VARCHAR(100) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_child_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_child_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
);

SET @has_legacy_child_columns := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME IN ('child_nickname', 'child_birth', 'keyword_text')
);

SET @has_legacy_disability_table := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_disability_types'
);

SET @sql := IF(
    @has_legacy_child_columns = 3 AND @has_legacy_disability_table = 1,
    'INSERT INTO child_profiles (user_id, nickname, birth, keyword_text)
     SELECT id, child_nickname, child_birth, keyword_text
     FROM users u
     WHERE (child_nickname IS NOT NULL
        OR child_birth IS NOT NULL
        OR keyword_text IS NOT NULL
        OR EXISTS (
            SELECT 1
            FROM user_disability_types udt
            WHERE udt.user_id = u.id
        ))
       AND NOT EXISTS (
           SELECT 1
           FROM child_profiles cp
           WHERE cp.user_id = u.id
       )',
    IF(
        @has_legacy_child_columns = 3,
        'INSERT INTO child_profiles (user_id, nickname, birth, keyword_text)
         SELECT id, child_nickname, child_birth, keyword_text
         FROM users u
         WHERE (child_nickname IS NOT NULL
            OR child_birth IS NOT NULL
            OR keyword_text IS NOT NULL)
           AND NOT EXISTS (
               SELECT 1
               FROM child_profiles cp
               WHERE cp.user_id = u.id
           )',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS child_disabilities (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    child_profile_id BIGINT      NOT NULL,
    disability_type  VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_child_disabilities_child_profile
        FOREIGN KEY (child_profile_id) REFERENCES child_profiles (id)
);

SET @sql := IF(
    @has_legacy_disability_table = 1,
    'INSERT INTO child_disabilities (child_profile_id, disability_type)
     SELECT
         cp.id,
         CASE udt.disability_type_id
             WHEN 1 THEN ''AUTISM''
             WHEN 2 THEN ''INTELLECTUAL_DISABILITY''
             WHEN 3 THEN ''CEREBRAL_PALSY''
             WHEN 4 THEN ''ADHD''
             WHEN 5 THEN ''DEVELOPMENTAL_DELAY''
             WHEN 6 THEN ''LANGUAGE_DISORDER''
             WHEN 7 THEN ''ETC''
             ELSE ''ETC''
         END
     FROM user_disability_types udt
     JOIN child_profiles cp ON cp.user_id = udt.user_id
     WHERE NOT EXISTS (
         SELECT 1
         FROM child_disabilities cd
         WHERE cd.child_profile_id = cp.id
           AND cd.disability_type = CASE udt.disability_type_id
               WHEN 1 THEN ''AUTISM''
               WHEN 2 THEN ''INTELLECTUAL_DISABILITY''
               WHEN 3 THEN ''CEREBRAL_PALSY''
               WHEN 4 THEN ''ADHD''
               WHEN 5 THEN ''DEVELOPMENTAL_DELAY''
               WHEN 6 THEN ''LANGUAGE_DISORDER''
               WHEN 7 THEN ''ETC''
               ELSE ''ETC''
           END
     )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS guardian_profiles (
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

SET @has_legacy_guardian_columns := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME IN (
          'region_id',
          'guardian_nickname',
          'guardian_type',
          'community_role_type',
          'point'
      )
);

SET @sql := IF(
    @has_legacy_guardian_columns = 5,
    'INSERT INTO guardian_profiles (user_id, nickname, region_id, guardian_type, community_role_type, point)
     SELECT id, guardian_nickname, region_id, guardian_type, community_role_type, point
     FROM users u
     WHERE (guardian_nickname IS NOT NULL
        OR region_id IS NOT NULL
        OR guardian_type IS NOT NULL
        OR community_role_type IS NOT NULL
        OR point <> 0)
       AND NOT EXISTS (
           SELECT 1
           FROM guardian_profiles gp
           WHERE gp.user_id = u.id
       )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_legacy_interest_column := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_interests'
      AND COLUMN_NAME = 'interest_category_id'
);

SET @has_user_interests_table := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_interests'
);

SET @has_user_interests_new_table := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_interests_new'
);

SET @sql := IF(
    @has_legacy_interest_column = 0
        AND @has_user_interests_table = 0
        AND @has_user_interests_new_table = 1,
    'RENAME TABLE user_interests_new TO user_interests',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_legacy_interest_column = 1,
    'DROP TABLE IF EXISTS user_interests_new',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_legacy_interest_column = 1,
    'CREATE TABLE IF NOT EXISTS user_interests_new (
         id                BIGINT      NOT NULL AUTO_INCREMENT,
         user_id           BIGINT      NOT NULL,
         interest_category VARCHAR(50) NOT NULL,
         PRIMARY KEY (id),
         CONSTRAINT fk_user_interests_user FOREIGN KEY (user_id) REFERENCES users (id)
     )',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(
    @has_legacy_interest_column = 1,
    'INSERT INTO user_interests_new (user_id, interest_category)
     SELECT
         user_id,
         CASE interest_category_id
             WHEN 1 THEN ''INSTITUTION''
             WHEN 2 THEN ''HOSPITAL''
             WHEN 3 THEN ''WELFARE''
             WHEN 4 THEN ''EMPLOYMENT''
             WHEN 5 THEN ''EDUCATION''
             ELSE ''INSTITUTION''
         END
     FROM user_interests',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_legacy_interest_column = 1, 'DROP TABLE user_interests', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_legacy_interest_column = 1, 'RENAME TABLE user_interests_new TO user_interests', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql := IF(@has_legacy_disability_table = 1, 'DROP TABLE user_disability_types', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @users_region_fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND CONSTRAINT_NAME = 'fk_users_region'
);
SET @sql := IF(@users_region_fk_exists = 1, 'ALTER TABLE users DROP FOREIGN KEY fk_users_region', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'child_nickname'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN child_nickname', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'child_birth'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN child_birth', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'keyword_text'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN keyword_text', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'region_id'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN region_id', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'guardian_nickname'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN guardian_nickname', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'guardian_type'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN guardian_type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'community_role_type'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN community_role_type', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'users'
      AND COLUMN_NAME = 'point'
);
SET @sql := IF(@column_exists = 1, 'ALTER TABLE users DROP COLUMN point', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
