-- 1. info_category 테이블 생성
CREATE TABLE info_category (
                               info_category_id BIGINT NOT NULL AUTO_INCREMENT,
                               parent_category VARCHAR(50) NOT NULL,
                               parent_category_ko VARCHAR(50) NOT NULL,
                               sub_category VARCHAR(50) NOT NULL,
                               sub_category_ko VARCHAR(50) NOT NULL,
                               PRIMARY KEY (info_category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 기존 info_item 테이블 구조 변경 (기존 category 컬럼 제거 및 외래키 컬럼 추가)
ALTER TABLE info_item DROP COLUMN category;
ALTER TABLE info_item ADD COLUMN info_category_id BIGINT NOT NULL AFTER external_id;

-- 3. 외래키(FK) 제약조건 설정
ALTER TABLE info_item
    ADD CONSTRAINT fk_info_item_info_category_id
        FOREIGN KEY (info_category_id) REFERENCES info_category (info_category_id);