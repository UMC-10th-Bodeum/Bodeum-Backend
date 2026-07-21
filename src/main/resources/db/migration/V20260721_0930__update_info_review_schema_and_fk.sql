-- 1. info_review 테이블 별점 타입 변경 (DECIMAL -> INT)
ALTER TABLE `info_review` MODIFY COLUMN `rating` INT NOT NULL;

-- 2. info_review_image 테이블 생성
CREATE TABLE `info_review_image` (
    `info_review_image_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `info_review_id` BIGINT NOT NULL,
    `image_url` VARCHAR(1000) NOT NULL,
    `display_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_info_review_image_review`
        FOREIGN KEY (`info_review_id`)
            REFERENCES `info_review` (`info_review_id`)
            ON DELETE CASCADE
);

-- 3. FK 제약조건 추가
ALTER TABLE `info_review`
    ADD CONSTRAINT `fk_info_review_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `info_review_helpful`
    ADD CONSTRAINT `fk_info_review_helpful_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `info_review_report`
    ADD CONSTRAINT `fk_info_review_report_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);

ALTER TABLE `info_scrap`
    ADD CONSTRAINT `fk_info_scrap_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`);
