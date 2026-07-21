-- 게시글 장애 유형 태그를 별도 장애 유형 테이블 FK가 아닌 Enum 문자열로 저장한다.
-- 기존 테이블이 없는 환경에서는 생성하고, legacy disability_type_id 컬럼이 있으면 데이터를 이관한다.

SET @cpdt_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
);

SET @cpdt_create_table_sql = IF(
    @cpdt_table_exists = 0,
    'CREATE TABLE community_post_disability_tag (
        community_post_disability_tag_id BIGINT NOT NULL AUTO_INCREMENT,
        post_id BIGINT NOT NULL,
        disability_type VARCHAR(50) NOT NULL,
        created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
        PRIMARY KEY (community_post_disability_tag_id),
        CONSTRAINT uk_community_post_disability_tag_post_type UNIQUE (post_id, disability_type),
        CONSTRAINT fk_community_post_disability_tag_post
            FOREIGN KEY (post_id) REFERENCES `post` (post_id)
    ) ENGINE=InnoDB',
    'SELECT 1'
);

PREPARE cpdt_create_table_stmt FROM @cpdt_create_table_sql;
EXECUTE cpdt_create_table_stmt;
DEALLOCATE PREPARE cpdt_create_table_stmt;

SET @cpdt_enum_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
      AND column_name = 'disability_type'
);

SET @cpdt_add_enum_column_sql = IF(
    @cpdt_enum_column_exists = 0,
    'ALTER TABLE community_post_disability_tag
        ADD COLUMN disability_type VARCHAR(50) NULL AFTER post_id',
    'SELECT 1'
);

PREPARE cpdt_add_enum_column_stmt FROM @cpdt_add_enum_column_sql;
EXECUTE cpdt_add_enum_column_stmt;
DEALLOCATE PREPARE cpdt_add_enum_column_stmt;

SET @cpdt_legacy_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
      AND column_name = 'disability_type_id'
);

SET @cpdt_backfill_sql = IF(
    @cpdt_legacy_column_exists > 0,
    'UPDATE community_post_disability_tag
     SET disability_type = CASE disability_type_id
         WHEN 1 THEN ''AUTISM''
         WHEN 2 THEN ''INTELLECTUAL_DISABILITY''
         WHEN 3 THEN ''CEREBRAL_PALSY''
         WHEN 4 THEN ''ADHD''
         WHEN 5 THEN ''DEVELOPMENTAL_DELAY''
         WHEN 6 THEN ''LANGUAGE_DISORDER''
         WHEN 7 THEN ''ETC''
         ELSE NULL
     END
     WHERE disability_type IS NULL',
    'SELECT 1'
);

PREPARE cpdt_backfill_stmt FROM @cpdt_backfill_sql;
EXECUTE cpdt_backfill_stmt;
DEALLOCATE PREPARE cpdt_backfill_stmt;

SET @cpdt_legacy_fk_name = (
    SELECT constraint_name
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
      AND column_name = 'disability_type_id'
      AND referenced_table_name IS NOT NULL
    LIMIT 1
);

SET @cpdt_drop_legacy_fk_sql = IF(
    @cpdt_legacy_fk_name IS NOT NULL,
    CONCAT(
        'ALTER TABLE community_post_disability_tag DROP FOREIGN KEY `',
        REPLACE(@cpdt_legacy_fk_name, '`', '``'),
        '`'
    ),
    'SELECT 1'
);

PREPARE cpdt_drop_legacy_fk_stmt FROM @cpdt_drop_legacy_fk_sql;
EXECUTE cpdt_drop_legacy_fk_stmt;
DEALLOCATE PREPARE cpdt_drop_legacy_fk_stmt;

SET @cpdt_drop_legacy_column_sql = IF(
    @cpdt_legacy_column_exists > 0,
    'ALTER TABLE community_post_disability_tag DROP COLUMN disability_type_id',
    'SELECT 1'
);

PREPARE cpdt_drop_legacy_column_stmt FROM @cpdt_drop_legacy_column_sql;
EXECUTE cpdt_drop_legacy_column_stmt;
DEALLOCATE PREPARE cpdt_drop_legacy_column_stmt;

SET @cpdt_created_at_exists = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
      AND column_name = 'created_at'
);

SET @cpdt_add_created_at_sql = IF(
    @cpdt_created_at_exists = 0,
    'ALTER TABLE community_post_disability_tag
        ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)',
    'SELECT 1'
);

PREPARE cpdt_add_created_at_stmt FROM @cpdt_add_created_at_sql;
EXECUTE cpdt_add_created_at_stmt;
DEALLOCATE PREPARE cpdt_add_created_at_stmt;

ALTER TABLE community_post_disability_tag
    MODIFY COLUMN disability_type VARCHAR(50) NOT NULL;

SET @cpdt_unique_exists = (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
      AND index_name = 'uk_community_post_disability_tag_post_type'
);

SET @cpdt_add_unique_sql = IF(
    @cpdt_unique_exists = 0,
    'ALTER TABLE community_post_disability_tag
        ADD CONSTRAINT uk_community_post_disability_tag_post_type UNIQUE (post_id, disability_type)',
    'SELECT 1'
);

PREPARE cpdt_add_unique_stmt FROM @cpdt_add_unique_sql;
EXECUTE cpdt_add_unique_stmt;
DEALLOCATE PREPARE cpdt_add_unique_stmt;

SET @cpdt_post_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.key_column_usage
    WHERE table_schema = DATABASE()
      AND table_name = 'community_post_disability_tag'
      AND column_name = 'post_id'
      AND referenced_table_name = 'post'
      AND referenced_column_name = 'post_id'
);

SET @cpdt_add_post_fk_sql = IF(
    @cpdt_post_fk_exists = 0,
    'ALTER TABLE community_post_disability_tag
        ADD CONSTRAINT fk_community_post_disability_tag_post
        FOREIGN KEY (post_id) REFERENCES `post` (post_id)',
    'SELECT 1'
);

PREPARE cpdt_add_post_fk_stmt FROM @cpdt_add_post_fk_sql;
EXECUTE cpdt_add_post_fk_stmt;
DEALLOCATE PREPARE cpdt_add_post_fk_stmt;
