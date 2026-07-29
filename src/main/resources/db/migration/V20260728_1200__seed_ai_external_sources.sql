-- AI 외부 검색에서 허용할 공식 사이트와 비영리단체 사이트를 등록한다.
-- 동일 도메인의 여러 상세 페이지는 하나의 외부 출처로 통합하고,
-- 실제로 답변에 인용된 상세 URL은 ai_external_document에 별도로 저장한다.

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
VALUES
    (
        '복지로',
        'WEBSITE',
        'https://www.bokjiro.go.kr/',
        'https://www.bokjiro.go.kr/index.do',
        '복지서비스 검색, 복지급여 안내와 온라인 신청 정보를 제공하는 공식 복지 포털',
        'GOVERNMENT',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '국립특수교육원 온맘',
        'WEBSITE',
        'https://www.nise.go.kr/',
        'https://www.nise.go.kr/onmam/front/index.do',
        '장애 자녀의 특수교육과 가족 지원 정보를 제공하는 국립특수교육원 부모지원 시스템',
        'GOVERNMENT',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '사회서비스 전자바우처',
        'WEBSITE',
        'https://www.socialservice.or.kr:444/',
        'https://www.socialservice.or.kr:444/',
        '사회서비스 전자바우처 제도와 발달재활서비스 제공기관 검색 정보를 제공하는 공식 사이트',
        'PUBLIC_INSTITUTION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '중앙장애아동·발달장애인지원센터',
        'WEBSITE',
        'https://www.broso.or.kr/',
        'https://www.broso.or.kr/mainPage.do',
        '장애아동과 발달장애인을 위한 지원사업, 교육, 지역 지원기관 정보를 제공하는 공식 사이트',
        'PUBLIC_INSTITUTION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '장애통계데이터포털',
        'WEBSITE',
        'https://koddi.or.kr/',
        'https://koddi.or.kr/stat/html/user/main/main',
        '한국장애인개발원이 장애인 관련 통계와 조사 자료를 제공하는 공식 데이터 포털',
        'PUBLIC_INSTITUTION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '국립재활원 장애인 건강·재활 정보포털',
        'WEBSITE',
        'https://nrc.go.kr/',
        'https://nrc.go.kr/nrc/main.do',
        '장애인 건강과 재활, 재활의료 및 관련 공공정보를 제공하는 국립재활원 공식 사이트',
        'GOVERNMENT',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '전국장애인부모연대',
        'WEBSITE',
        'https://www.bumo.or.kr/',
        'https://www.bumo.or.kr/',
        '장애인 가족 지원 정보와 지역별 복지기관 검색 정보를 제공하는 비영리단체 사이트',
        'NONPROFIT_ORGANIZATION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '한국장애인부모회',
        'WEBSITE',
        'https://www.kpat.or.kr/',
        'https://www.kpat.or.kr/',
        '장애인과 가족을 대상으로 교육, 상담, 직업재활, 공공후견 및 지역 기관 정보를 제공하는 비영리단체 사이트',
        'NONPROFIT_ORGANIZATION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '푸르메재단',
        'WEBSITE',
        'https://purme.org/',
        'https://purme.org/',
        '장애인의 재활과 자립을 위한 의료·복지 지원사업 정보를 제공하는 비영리재단 사이트',
        'NONPROFIT_ORGANIZATION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '한국자폐인사랑협회',
        'WEBSITE',
        'https://www.autismkorea.kr/',
        'https://www.autismkorea.kr/main.php',
        '자폐성 장애인과 가족을 위한 정책, 교육, 복지 및 권익 지원 정보를 제공하는 비영리단체 사이트',
        'NONPROFIT_ORGANIZATION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '경기도 장애인가족지원센터',
        'WEBSITE',
        'http://ggdf.co.kr/',
        'http://ggdf.co.kr/',
        '경기도 장애인 가족을 위한 상담, 정보 연계와 가족 역량 강화 사업을 안내하는 지원센터 사이트',
        'PUBLIC_INSTITUTION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '정부24',
        'WEBSITE',
        'https://www.gov.kr/',
        'https://www.gov.kr/',
        '장애인 등록을 포함한 정부 서비스와 민원 신청, 증명서 발급 정보를 제공하는 정부 포털',
        'GOVERNMENT',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '보건복지부',
        'WEBSITE',
        'https://www.mohw.go.kr/',
        'https://www.mohw.go.kr/',
        '보건복지 정책과 장애인 건강·의료비 지원사업 정보를 제공하는 중앙행정기관 공식 사이트',
        'GOVERNMENT',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '국민건강보험공단',
        'WEBSITE',
        'https://www.nhis.or.kr/',
        'https://www.nhis.or.kr/nhis/index.do',
        '건강보험과 본인부담액상한제, 재난적의료비 지원사업 정보를 제공하는 공공기관 공식 사이트',
        'PUBLIC_INSTITUTION',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    ),
    (
        '보건복지상담센터',
        'WEBSITE',
        'https://www.129.go.kr/',
        'https://www.129.go.kr/',
        '보건복지 제도와 긴급복지, 장애인 지원 등에 관한 상담 정보를 제공하는 공식 상담센터 사이트',
        'GOVERNMENT',
        TRUE,
        CURRENT_TIMESTAMP(6),
        CURRENT_TIMESTAMP(6)
    )
ON DUPLICATE KEY UPDATE
    entry_url = VALUES(entry_url),
    description = VALUES(description),
    authority_level = VALUES(authority_level),
    is_active = TRUE,
    updated_at = CURRENT_TIMESTAMP(6);

-- 데모 활동 지역인 경기도에서 바로 안내할 수 있도록 경기지역센터 페이지를
-- 중앙장애아동·발달장애인지원센터의 외부 문서로 미리 등록한다.
INSERT INTO ai_external_document (
    ai_external_source_id,
    title,
    source_url,
    source_url_hash,
    source_updated_at,
    created_at,
    updated_at
)
SELECT
    source.ai_external_source_id,
    '경기지역 발달장애인지원센터',
    'https://www.broso.or.kr/gyeonggi/mainPage.do',
    SHA2('https://www.broso.or.kr/gyeonggi/mainPage.do', 256),
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM ai_external_source source
WHERE source.base_url = 'https://www.broso.or.kr/'
ON DUPLICATE KEY UPDATE
    ai_external_source_id = VALUES(ai_external_source_id),
    title = VALUES(title),
    source_url = VALUES(source_url),
    updated_at = CURRENT_TIMESTAMP(6);

-- 장애 진단 후 첫 단계 추천 질문의 검수 답변에서 사용하는 공식 상세 페이지를
-- 실제 응답 출처로 연결할 수 있도록 외부 문서로 미리 등록한다.
INSERT INTO ai_external_document (
    ai_external_source_id,
    title,
    source_url,
    source_url_hash,
    source_updated_at,
    created_at,
    updated_at
)
SELECT
    source.ai_external_source_id,
    document.title,
    document.source_url,
    SHA2(document.source_url, 256),
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM ai_external_source source
JOIN (
    SELECT
        'https://www.mohw.go.kr/' AS base_url,
        '2026년 장애아동가족지원 사업안내' AS title,
        CONCAT(
            'https://www.mohw.go.kr/board.es?mid=a10411010100&bid=0019',
            '&act=view&list_no=1489566&tag=&nPage=1'
        ) AS source_url
    UNION ALL
    SELECT
        'https://www.socialservice.or.kr:444/',
        '발달재활서비스',
        'https://www.socialservice.or.kr:444/user/htmlEditor/view2.do?p_sn=11'
    UNION ALL
    SELECT
        'https://www.bokjiro.go.kr/',
        '복지서비스 신청',
        CONCAT(
            'https://www.bokjiro.go.kr/ssis-tbu/twatzzza/intgSearch/',
            'moveTWZZ01000M.do'
        )
    UNION ALL
    SELECT
        'https://www.129.go.kr/',
        '보건복지상담센터',
        'https://www.129.go.kr/'
) document ON document.base_url = source.base_url
ON DUPLICATE KEY UPDATE
    ai_external_source_id = VALUES(ai_external_source_id),
    title = VALUES(title),
    source_url = VALUES(source_url),
    updated_at = CURRENT_TIMESTAMP(6);

-- 장애아동 의료비 지원 추천 질문의 검수 답변에서 사용하는 공식 상세 페이지를
-- 실제 응답 출처로 연결할 수 있도록 외부 문서로 미리 등록한다.
INSERT INTO ai_external_document (
    ai_external_source_id,
    title,
    source_url,
    source_url_hash,
    source_updated_at,
    created_at,
    updated_at
)
SELECT
    source.ai_external_source_id,
    document.title,
    document.source_url,
    SHA2(document.source_url, 256),
    NULL,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM ai_external_source source
JOIN (
    SELECT
        'https://www.mohw.go.kr/' AS base_url,
        '장애인 의료비 지원' AS title,
        'https://www.mohw.go.kr/menu.es?mid=a10710060700' AS source_url
    UNION ALL
    SELECT
        'https://www.nhis.or.kr/',
        '본인부담액상한제',
        CONCAT(
            'https://www.nhis.or.kr/nhis/minwon/minwonServiceBoard.do',
            '?mode=list&etcChar1=446&etcChar2=447&etcChar3=448',
            '&categories1=446%2C447%2C448&articleLimit=12',
            '&nhisOrderTy=ORDER_DT&srSearchVal=%EB%B3%B8%EC%9D%B8',
            '%EB%B6%80%EB%8B%B4%EA%B8%88'
        )
    UNION ALL
    SELECT
        'https://www.nhis.or.kr/',
        '재난적의료비 지원사업',
        CONCAT(
            'https://www.nhis.or.kr/nhis/minwon/minwonServiceBoard.do',
            '?mode=view&articleNo=11009687&article.offset=0',
            '&articleLimit=12&srSearchVal=%EB%B3%B8%EC%9D%B8',
            '%EB%B6%80%EB%8B%B4'
        )
) document ON document.base_url = source.base_url
ON DUPLICATE KEY UPDATE
    ai_external_source_id = VALUES(ai_external_source_id),
    title = VALUES(title),
    source_url = VALUES(source_url),
    updated_at = CURRENT_TIMESTAMP(6);
