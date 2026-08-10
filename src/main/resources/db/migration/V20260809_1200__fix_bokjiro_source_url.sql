-- 복지로의 기존 대표 진입 URL(/index.do)이 오류 페이지로 이동하므로
-- 현재 사용되는 메인 페이지 URL로 변경한다.
UPDATE ai_external_source
SET entry_url = 'https://www.bokjiro.go.kr/ssis-tbu/',
    updated_at = CURRENT_TIMESTAMP(6)
WHERE base_url = 'https://www.bokjiro.go.kr/'
  AND entry_url = 'https://www.bokjiro.go.kr/index.do';

-- 이미 저장된 AI 응답은 출처 URL을 스냅샷으로 보관하므로
-- 기존 대화 이력의 출처 카드도 정상적으로 열리도록 함께 변경한다.
UPDATE ai_response_source
SET source_url = 'https://www.bokjiro.go.kr/ssis-tbu/'
WHERE source_type = 'SITE'
  AND source_url = 'https://www.bokjiro.go.kr/index.do';
