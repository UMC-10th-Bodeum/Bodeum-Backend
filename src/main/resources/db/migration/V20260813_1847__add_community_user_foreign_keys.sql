-- 커뮤니티 테이블의 작성자·사용자 참조를 users.id와 일치시킨다.
-- 탈퇴 사용자는 users 행을 소프트 삭제하므로 사용자 삭제 시 연관 데이터를 제거하지 않는다.

SET @post_user_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'users'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @post_user_fk_sql = IF(
    @post_user_fk_exists = 0,
    'ALTER TABLE `post`
        ADD CONSTRAINT fk_post_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE post_user_fk_statement FROM @post_user_fk_sql;
EXECUTE post_user_fk_statement;
DEALLOCATE PREPARE post_user_fk_statement;

SET @comments_user_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'users'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @comments_user_fk_sql = IF(
    @comments_user_fk_exists = 0,
    'ALTER TABLE comments
        ADD CONSTRAINT fk_comments_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE comments_user_fk_statement FROM @comments_user_fk_sql;
EXECUTE comments_user_fk_statement;
DEALLOCATE PREPARE comments_user_fk_statement;

SET @post_like_user_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_like'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'users'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @post_like_user_fk_sql = IF(
    @post_like_user_fk_exists = 0,
    'ALTER TABLE post_like
        ADD CONSTRAINT fk_post_like_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE post_like_user_fk_statement FROM @post_like_user_fk_sql;
EXECUTE post_like_user_fk_statement;
DEALLOCATE PREPARE post_like_user_fk_statement;

SET @comment_like_user_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comment_like'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'users'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @comment_like_user_fk_sql = IF(
    @comment_like_user_fk_exists = 0,
    'ALTER TABLE comment_like
        ADD CONSTRAINT fk_comment_like_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE comment_like_user_fk_statement FROM @comment_like_user_fk_sql;
EXECUTE comment_like_user_fk_statement;
DEALLOCATE PREPARE comment_like_user_fk_statement;

SET @post_scrap_user_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'post_scrap'
      AND COLUMN_NAME = 'user_id'
      AND REFERENCED_TABLE_NAME = 'users'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @post_scrap_user_fk_sql = IF(
    @post_scrap_user_fk_exists = 0,
    'ALTER TABLE post_scrap
        ADD CONSTRAINT fk_post_scrap_user
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE post_scrap_user_fk_statement FROM @post_scrap_user_fk_sql;
EXECUTE post_scrap_user_fk_statement;
DEALLOCATE PREPARE post_scrap_user_fk_statement;
