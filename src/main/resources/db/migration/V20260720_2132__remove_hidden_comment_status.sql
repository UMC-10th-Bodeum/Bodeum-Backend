-- 댓글 숨김 상태를 사용하지 않으므로 기존 숨김 댓글을 논리 삭제 상태로 이관한다.
UPDATE comments
SET status = 'DELETED',
    deleted_at = COALESCE(deleted_at, CURRENT_TIMESTAMP(6))
WHERE status = 'HIDDEN';
