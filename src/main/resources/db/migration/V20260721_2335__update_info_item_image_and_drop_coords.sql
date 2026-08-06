-- 1. InfoRegion 테이블 신규 생성
CREATE TABLE info_region (
                             info_region_id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             sido VARCHAR(30) NOT NULL,
                             sigungu VARCHAR(50) NOT NULL
);

-- 2. InfoItem 테이블 위/경도 컬럼 삭제 및 대표 이미지(image_url) 컬럼 추가
ALTER TABLE info_item
    DROP COLUMN latitude,
    DROP COLUMN longitude,
    ADD COLUMN image_url VARCHAR(1000) NULL COMMENT '대표 이미지 URL';