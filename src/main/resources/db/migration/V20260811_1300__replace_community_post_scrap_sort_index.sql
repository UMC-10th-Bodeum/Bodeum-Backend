DROP INDEX idx_post_active_scrap_order ON `post`;

CREATE INDEX idx_post_active_like_order
    ON `post` (status, deleted_at, like_count DESC, created_at DESC, post_id DESC);
