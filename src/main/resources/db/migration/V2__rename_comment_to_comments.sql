-- `comment`는 MySQL 예약어라 테이블명을 comments로 변경한다.
-- RENAME TABLE은 이 테이블을 참조하는 FK(CommentLike.comment_id, Comment.parent_comment_id)도 자동 갱신한다.
-- 운영 DB는 장애 복구 중 이미 수동 rename 되었을 수 있으므로, `comment`가 남아 있을 때만 rename한다.
SET @comment_table_exists = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'comment'
);

SET @rename_comment_sql = IF(
    @comment_table_exists > 0,
    'RENAME TABLE `comment` TO comments',
    'SELECT 1'
);

PREPARE rename_comment_stmt FROM @rename_comment_sql;
EXECUTE rename_comment_stmt;
DEALLOCATE PREPARE rename_comment_stmt;
