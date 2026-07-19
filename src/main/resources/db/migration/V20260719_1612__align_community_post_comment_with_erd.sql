ALTER TABLE `post`
    ADD COLUMN is_question BOOLEAN NOT NULL DEFAULT FALSE AFTER content,
    ADD COLUMN view_count INT NOT NULL DEFAULT 0 AFTER is_question,
    ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER view_count,
    ADD COLUMN comment_count INT NOT NULL DEFAULT 0 AFTER like_count,
    ADD COLUMN scrap_count INT NOT NULL DEFAULT 0 AFTER comment_count,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER scrap_count,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;

ALTER TABLE comments
    ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER is_accepted,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER like_count,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER updated_at;

-- 기존 질문 게시판 데이터는 질문글로 이관한다.
UPDATE `post`
SET is_question = TRUE
WHERE board_type = 'INFORMATION_QUESTION';

-- 기존 관계 테이블을 기준으로 비정규화 카운터의 초기값을 채운다.
UPDATE `post` p
SET p.like_count = (
        SELECT COUNT(*)
        FROM post_like pl
        WHERE pl.post_id = p.post_id
    ),
    p.comment_count = (
        SELECT COUNT(*)
        FROM comments c
        WHERE c.post_id = p.post_id
    ),
    p.scrap_count = (
        SELECT COUNT(*)
        FROM post_scrap ps
        WHERE ps.post_id = p.post_id
    );

UPDATE comments c
SET c.like_count = (
    SELECT COUNT(*)
    FROM comment_like cl
    WHERE cl.comment_id = c.comment_id
);
