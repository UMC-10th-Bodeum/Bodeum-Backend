CREATE INDEX idx_post_active_view_order
    ON `post` (status, deleted_at, view_count DESC, created_at DESC, post_id DESC);

CREATE INDEX idx_post_active_scrap_order
    ON `post` (status, deleted_at, scrap_count DESC, created_at DESC, post_id DESC);

CREATE INDEX idx_post_active_comment_order
    ON `post` (status, deleted_at, comment_count DESC, created_at DESC, post_id DESC);
