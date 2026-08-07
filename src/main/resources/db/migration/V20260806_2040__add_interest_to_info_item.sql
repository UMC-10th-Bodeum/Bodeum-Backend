-- 1. info_item 테이블에 interest 컬럼 추가
ALTER TABLE info_item ADD COLUMN interest VARCHAR(50);

-- 2. 기존 공공데이터 info_category_id 기반 interest 컬럼 데이터 마이그레이션

-- HOSPITAL_HEALTH (안심 병원, 건강): [3, 4]
UPDATE info_item
SET interest = 'HOSPITAL_HEALTH'
WHERE info_category_id IN (3, 4);

-- WELFARE_SUBSIDY (맞춤 복지 지원금): [11, 12, 13, 19, 21]
UPDATE info_item
SET interest = 'WELFARE_SUBSIDY'
WHERE info_category_id IN (11, 12, 13, 19, 21);

-- GROWTH_EDUCATION (성장, 교육): [15, 16, 17]
UPDATE info_item
SET interest = 'GROWTH_EDUCATION'
WHERE info_category_id IN (15, 16, 17);

-- PARENTING_COMMUNICATION (육아 상담, 소통): [6, 7, 9]
UPDATE info_item
SET interest = 'PARENTING_COMMUNICATION'
WHERE info_category_id IN (6, 7, 9);