-- enum 저장 컬럼 native ENUM → VARCHAR 정합화 (#97)
--
-- 배경: Spring Boot 4/Hibernate 6은 MySQL 대상 @Enumerated(EnumType.STRING)를 native ENUM(...)
--   타입으로 DDL 생성한다. 초기 ddl-auto=update가 만든 컬럼들이 "생성 시점 값 목록"에 고정된
--   native ENUM으로 남아, 이후 Java enum 값 변경이 반영되지 않는다.
--   특히 user_interests.interest_category는 옛 5개 값에 고정되어, PR #70이 ALTER 없이 데이터만
--   바꾸자 새 값(WELFARE_SUBSIDY 등)이 비 strict 환경에서 ''로 truncate되었다.
--
-- 조치: 코드 매핑(@Enumerated(STRING), @Column(length/nullable))에 맞춰 전부 VARCHAR로 통일한다.
--   ENUM→VARCHAR 변환은 유효 값을 문자열로 보존하며, 이미 VARCHAR면 사실상 no-op(idempotent).

-- auth / oauth
ALTER TABLE oauth_states           MODIFY COLUMN provider            VARCHAR(20)  NOT NULL;
ALTER TABLE users                  MODIFY COLUMN provider            VARCHAR(20)  NOT NULL;
ALTER TABLE users                  MODIFY COLUMN status              VARCHAR(20)  NOT NULL;

-- user / onboarding
ALTER TABLE user_interests         MODIFY COLUMN interest_category   VARCHAR(50)  NOT NULL;
ALTER TABLE child_disabilities     MODIFY COLUMN disability_type     VARCHAR(50)  NOT NULL;
ALTER TABLE guardian_profiles      MODIFY COLUMN guardian_type       VARCHAR(50)  NULL;
ALTER TABLE guardian_profiles      MODIFY COLUMN community_role_type VARCHAR(50)  NULL;
ALTER TABLE guardian_point         MODIFY COLUMN badge_level         VARCHAR(30)  NOT NULL;
ALTER TABLE guardian_point_history MODIFY COLUMN point_type          VARCHAR(50)  NOT NULL;

-- community
ALTER TABLE post                   MODIFY COLUMN board_type          VARCHAR(40)  NOT NULL;
ALTER TABLE post                   MODIFY COLUMN anonymity_type      VARCHAR(30)  NOT NULL;

-- info
ALTER TABLE info_operating_hour    MODIFY COLUMN day_of_week         VARCHAR(255) NOT NULL;
ALTER TABLE info_review_report     MODIFY COLUMN reason_type         VARCHAR(255) NOT NULL;
ALTER TABLE info_review_report     MODIFY COLUMN status              VARCHAR(255) NOT NULL;

-- news
ALTER TABLE news                   MODIFY COLUMN news_type           VARCHAR(30)  NOT NULL;
ALTER TABLE news                   MODIFY COLUMN recruitment_status  VARCHAR(30)  NULL;
ALTER TABLE news_category          MODIFY COLUMN news_type           VARCHAR(30)  NOT NULL;
ALTER TABLE news_source            MODIFY COLUMN source_type         VARCHAR(30)  NOT NULL;

-- search / ai
ALTER TABLE search_log             MODIFY COLUMN search_type         VARCHAR(255) NOT NULL;
ALTER TABLE ai_message             MODIFY COLUMN sender_type         VARCHAR(255) NOT NULL;
ALTER TABLE ai_feedback            MODIFY COLUMN feedback_type       VARCHAR(255) NOT NULL;
ALTER TABLE ai_feedback_reason     MODIFY COLUMN reason              VARCHAR(255) NOT NULL;

-- user_interests: 설계 기준 4개 값 외 잔여·'' 행 정리 (해당 유저는 온보딩에서 재선택)
DELETE FROM user_interests
WHERE interest_category NOT IN (
    'WELFARE_SUBSIDY', 'HOSPITAL_HEALTH', 'PARENTING_COMMUNICATION', 'GROWTH_EDUCATION'
);
