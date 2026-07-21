-- #60 리팩터에서 InfoCategory 엔티티 필드를 parent_category → main_category 로 변경했으나
-- 대응하는 DB 컬럼 rename 마이그레이션이 누락되어, Hibernate ddl-auto=validate 가
-- "missing column [main_category]" 로 부팅에 실패했다.
-- 엔티티는 @Enumerated(STRING) @Column(length = 50) 이므로 형제 컬럼들과 동일하게 VARCHAR(50) 로 맞춘다.
ALTER TABLE info_category
    CHANGE COLUMN parent_category    main_category    VARCHAR(50) NOT NULL,
    CHANGE COLUMN parent_category_ko main_category_ko VARCHAR(50) NOT NULL;
