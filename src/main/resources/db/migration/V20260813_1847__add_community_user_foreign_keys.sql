-- 커뮤니티 테이블의 작성자·사용자 참조를 users.id와 일치시킨다.
-- 탈퇴 사용자는 users 행을 소프트 삭제하므로 사용자 삭제 시 연관 데이터를 제거하지 않는다.
-- 배포 전 각 테이블의 고아 user_id가 없는지 확인한다. 고아 행이 있으면 FK 추가가 실패하므로
-- 데이터를 임의 정리하지 말고 원인을 확인한 뒤 별도 조치한다.

SET @post_user_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'post'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @post_user_fk_rules_match = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
      ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
     AND rc.TABLE_NAME = kcu.TABLE_NAME
     AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'post'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
      AND rc.UPDATE_RULE = 'RESTRICT'
      AND rc.DELETE_RULE = 'RESTRICT'
);

SET @post_user_fk_drop_sql = IF(
    @post_user_fk_name IS NOT NULL AND @post_user_fk_rules_match = 0,
    CONCAT(
        'ALTER TABLE `post` DROP FOREIGN KEY `',
        REPLACE(@post_user_fk_name, '`', '``'),
        '`'
    ),
    'SELECT 1'
);

PREPARE post_user_fk_drop_statement FROM @post_user_fk_drop_sql;
EXECUTE post_user_fk_drop_statement;
DEALLOCATE PREPARE post_user_fk_drop_statement;

SET @post_user_fk_add_sql = IF(
    @post_user_fk_rules_match = 0,
    'ALTER TABLE `post`
        ADD CONSTRAINT fk_post_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE post_user_fk_add_statement FROM @post_user_fk_add_sql;
EXECUTE post_user_fk_add_statement;
DEALLOCATE PREPARE post_user_fk_add_statement;

SET @comments_user_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'comments'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @comments_user_fk_rules_match = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
      ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
     AND rc.TABLE_NAME = kcu.TABLE_NAME
     AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'comments'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
      AND rc.UPDATE_RULE = 'RESTRICT'
      AND rc.DELETE_RULE = 'RESTRICT'
);

SET @comments_user_fk_drop_sql = IF(
    @comments_user_fk_name IS NOT NULL AND @comments_user_fk_rules_match = 0,
    CONCAT(
        'ALTER TABLE comments DROP FOREIGN KEY `',
        REPLACE(@comments_user_fk_name, '`', '``'),
        '`'
    ),
    'SELECT 1'
);

PREPARE comments_user_fk_drop_statement FROM @comments_user_fk_drop_sql;
EXECUTE comments_user_fk_drop_statement;
DEALLOCATE PREPARE comments_user_fk_drop_statement;

SET @comments_user_fk_add_sql = IF(
    @comments_user_fk_rules_match = 0,
    'ALTER TABLE comments
        ADD CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE comments_user_fk_add_statement FROM @comments_user_fk_add_sql;
EXECUTE comments_user_fk_add_statement;
DEALLOCATE PREPARE comments_user_fk_add_statement;

SET @post_like_user_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'post_like'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @post_like_user_fk_rules_match = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
      ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
     AND rc.TABLE_NAME = kcu.TABLE_NAME
     AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'post_like'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
      AND rc.UPDATE_RULE = 'RESTRICT'
      AND rc.DELETE_RULE = 'RESTRICT'
);

SET @post_like_user_fk_drop_sql = IF(
    @post_like_user_fk_name IS NOT NULL AND @post_like_user_fk_rules_match = 0,
    CONCAT(
        'ALTER TABLE post_like DROP FOREIGN KEY `',
        REPLACE(@post_like_user_fk_name, '`', '``'),
        '`'
    ),
    'SELECT 1'
);

PREPARE post_like_user_fk_drop_statement FROM @post_like_user_fk_drop_sql;
EXECUTE post_like_user_fk_drop_statement;
DEALLOCATE PREPARE post_like_user_fk_drop_statement;

SET @post_like_user_fk_add_sql = IF(
    @post_like_user_fk_rules_match = 0,
    'ALTER TABLE post_like
        ADD CONSTRAINT fk_post_like_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE post_like_user_fk_add_statement FROM @post_like_user_fk_add_sql;
EXECUTE post_like_user_fk_add_statement;
DEALLOCATE PREPARE post_like_user_fk_add_statement;

SET @comment_like_user_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'comment_like'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @comment_like_user_fk_rules_match = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
      ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
     AND rc.TABLE_NAME = kcu.TABLE_NAME
     AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'comment_like'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
      AND rc.UPDATE_RULE = 'RESTRICT'
      AND rc.DELETE_RULE = 'RESTRICT'
);

SET @comment_like_user_fk_drop_sql = IF(
    @comment_like_user_fk_name IS NOT NULL AND @comment_like_user_fk_rules_match = 0,
    CONCAT(
        'ALTER TABLE comment_like DROP FOREIGN KEY `',
        REPLACE(@comment_like_user_fk_name, '`', '``'),
        '`'
    ),
    'SELECT 1'
);

PREPARE comment_like_user_fk_drop_statement FROM @comment_like_user_fk_drop_sql;
EXECUTE comment_like_user_fk_drop_statement;
DEALLOCATE PREPARE comment_like_user_fk_drop_statement;

SET @comment_like_user_fk_add_sql = IF(
    @comment_like_user_fk_rules_match = 0,
    'ALTER TABLE comment_like
        ADD CONSTRAINT fk_comment_like_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE comment_like_user_fk_add_statement FROM @comment_like_user_fk_add_sql;
EXECUTE comment_like_user_fk_add_statement;
DEALLOCATE PREPARE comment_like_user_fk_add_statement;

SET @post_scrap_user_fk_name = (
    SELECT kcu.CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE kcu
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'post_scrap'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
    LIMIT 1
);

SET @post_scrap_user_fk_rules_match = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE kcu
    JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
      ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
     AND rc.TABLE_NAME = kcu.TABLE_NAME
     AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
    WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
      AND kcu.TABLE_NAME = 'post_scrap'
      AND kcu.COLUMN_NAME = 'user_id'
      AND kcu.REFERENCED_TABLE_NAME = 'users'
      AND kcu.REFERENCED_COLUMN_NAME = 'id'
      AND rc.UPDATE_RULE = 'RESTRICT'
      AND rc.DELETE_RULE = 'RESTRICT'
);

SET @post_scrap_user_fk_drop_sql = IF(
    @post_scrap_user_fk_name IS NOT NULL AND @post_scrap_user_fk_rules_match = 0,
    CONCAT(
        'ALTER TABLE post_scrap DROP FOREIGN KEY `',
        REPLACE(@post_scrap_user_fk_name, '`', '``'),
        '`'
    ),
    'SELECT 1'
);

PREPARE post_scrap_user_fk_drop_statement FROM @post_scrap_user_fk_drop_sql;
EXECUTE post_scrap_user_fk_drop_statement;
DEALLOCATE PREPARE post_scrap_user_fk_drop_statement;

SET @post_scrap_user_fk_add_sql = IF(
    @post_scrap_user_fk_rules_match = 0,
    'ALTER TABLE post_scrap
        ADD CONSTRAINT fk_post_scrap_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE post_scrap_user_fk_add_statement FROM @post_scrap_user_fk_add_sql;
EXECUTE post_scrap_user_fk_add_statement;
DEALLOCATE PREPARE post_scrap_user_fk_add_statement;
