-- Local-only seed data for the AI RAG integration test.
-- Do not register this file as a Flyway migration.

USE bodeum;
SET NAMES utf8mb4;

START TRANSACTION;

-- -----------------------------------------------------------------------------
-- INFO: a question containing "보듬테스트 언어재활센터" should cite INFO.
-- -----------------------------------------------------------------------------
INSERT INTO info_category (
    main_category,
    main_category_ko,
    sub_category,
    sub_category_ko
)
SELECT
    'INSTITUTION',
    '기관',
    'THERAPY_REHAB',
    '치료·재활 기관'
WHERE NOT EXISTS (
    SELECT 1
    FROM info_category
    WHERE main_category = 'INSTITUTION'
      AND sub_category = 'THERAPY_REHAB'
);

SET @ai_test_info_category_id = (
    SELECT info_category_id
    FROM info_category
    WHERE main_category = 'INSTITUTION'
      AND sub_category = 'THERAPY_REHAB'
    ORDER BY info_category_id
    LIMIT 1
);

INSERT INTO info_item (
    external_id,
    info_category_id,
    name,
    introduction,
    address,
    sido,
    sigungu,
    phone,
    homepage_url,
    view_count,
    scrap_count,
    review_count,
    synced_at,
    created_at,
    updated_at
)
SELECT
    'AI-RAG-LOCAL-INFO-001',
    @ai_test_info_category_id,
    '보듬테스트 수원 언어재활센터',
    'AI RAG 로컬 테스트용 기관입니다. 경기도 수원시에서 장애아동 언어재활 상담과 보호자 초기 상담을 제공합니다.',
    '경기도 수원시 팔달구 테스트로 55',
    '경기도',
    '수원시',
    '031-111-1155',
    'https://example.com/bodeum-ai-test/info',
    0,
    0,
    0,
    NOW(6),
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM info_item
    WHERE external_id = 'AI-RAG-LOCAL-INFO-001'
);

-- -----------------------------------------------------------------------------
-- NEWS: a question containing "보듬테스트 여름방학 가족 프로그램" should cite NEWS.
-- -----------------------------------------------------------------------------
INSERT INTO news_category (
    news_type,
    name,
    sort_order,
    is_active
)
SELECT
    'ACTIVITY',
    'AI RAG 테스트 프로그램',
    999,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM news_category
    WHERE news_type = 'ACTIVITY'
      AND name = 'AI RAG 테스트 프로그램'
);

SET @ai_test_news_category_id = (
    SELECT news_category_id
    FROM news_category
    WHERE news_type = 'ACTIVITY'
      AND name = 'AI RAG 테스트 프로그램'
    ORDER BY news_category_id
    LIMIT 1
);

INSERT INTO news_source (
    source_type,
    name,
    base_url,
    list_url,
    is_active,
    last_synced_at,
    created_at,
    updated_at
)
SELECT
    'PUBLIC_API',
    '보듬 AI RAG 로컬 테스트 소식 출처',
    'https://example.com/bodeum-ai-test',
    'https://example.com/bodeum-ai-test/news',
    TRUE,
    NOW(6),
    NOW(6),
    NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM news_source
    WHERE name = '보듬 AI RAG 로컬 테스트 소식 출처'
);

SET @ai_test_news_source_id = (
    SELECT news_source_id
    FROM news_source
    WHERE name = '보듬 AI RAG 로컬 테스트 소식 출처'
    ORDER BY news_source_id
    LIMIT 1
);

INSERT INTO news (
    content_category_id,
    news_source_id,
    external_item_id,
    region_id,
    organization_id,
    title,
    summary,
    content,
    source_name,
    published_at,
    original_url,
    thumbnail_url,
    news_type,
    recruitment_status,
    view_count,
    scrap_count,
    is_active,
    target_audience,
    contact,
    manager,
    program_start_date,
    program_end_date,
    apply_start_date,
    apply_end_date,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    @ai_test_news_category_id,
    @ai_test_news_source_id,
    'AI-RAG-LOCAL-NEWS-001',
    NULL,
    'AI-RAG-LOCAL-ORG',
    '2026 보듬테스트 수원 장애아동 여름방학 가족 프로그램 모집',
    'AI RAG 로컬 테스트용 소식으로 신청 기간과 프로그램 일정을 확인할 수 있습니다.',
    '경기도 수원시에 거주하는 장애아동 가족을 대상으로 여름방학 가족 프로그램 참가자를 모집합니다. 신청은 2026년 7월 20일부터 7월 31일까지이며 프로그램은 2026년 8월 10일부터 8월 14일까지 진행됩니다.',
    '보듬 AI RAG 로컬 테스트 기관',
    '2026-07-20 09:00:00',
    'https://example.com/bodeum-ai-test/news/001',
    NULL,
    'ACTIVITY',
    'OPEN',
    0,
    0,
    TRUE,
    '경기도 수원시 거주 장애아동 가족',
    '031-222-2255',
    '테스트 담당자',
    '2026-08-10',
    '2026-08-14',
    '2026-07-20',
    '2026-07-31',
    NOW(6),
    NOW(6),
    NULL
WHERE NOT EXISTS (
    SELECT 1
    FROM news
    WHERE news_source_id = @ai_test_news_source_id
      AND external_item_id = 'AI-RAG-LOCAL-NEWS-001'
);

-- -----------------------------------------------------------------------------
-- SITE: only the allowed domain is inserted in advance.
-- ai_external_resource must be created automatically after an external search.
-- -----------------------------------------------------------------------------
INSERT INTO ai_external_source (
    name,
    source_type,
    base_url,
    entry_url,
    description,
    authority_level,
    is_active,
    created_at,
    updated_at
)
VALUES (
    '한국장애인부모회',
    'WEBSITE',
    'https://www.kpat.or.kr/',
    'https://www.kpat.or.kr/',
    '장애인과 가족을 대상으로 교육, 상담, 직업재활, 공공후견 및 지역 기관 정보를 제공하는 단체 사이트',
    'NONPROFIT_ORGANIZATION',
    TRUE,
    NOW(6),
    NOW(6)
)
ON DUPLICATE KEY UPDATE
    entry_url = VALUES(entry_url),
    description = VALUES(description),
    authority_level = VALUES(authority_level),
    is_active = TRUE,
    updated_at = NOW(6);

COMMIT;

-- Verification
SELECT
    info_item_id,
    external_id,
    name,
    info_category_id
FROM info_item
WHERE external_id = 'AI-RAG-LOCAL-INFO-001';

SELECT
    news_id,
    external_item_id,
    title,
    news_source_id,
    content_category_id
FROM news
WHERE external_item_id = 'AI-RAG-LOCAL-NEWS-001';

SELECT
    ai_external_source_id,
    name,
    base_url,
    is_active
FROM ai_external_source
WHERE name = '한국장애인부모회';

SELECT
    ai_external_resource_id,
    title,
    source_url
FROM ai_external_resource
ORDER BY ai_external_resource_id DESC;
