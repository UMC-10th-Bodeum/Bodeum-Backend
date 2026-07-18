-- `comment`는 MySQL 예약어라 테이블명을 comments로 변경한다.
-- RENAME TABLE은 이 테이블을 참조하는 FK(CommentLike.comment_id, Comment.parent_comment_id)도 자동 갱신한다.
RENAME TABLE `comment` TO comments;
