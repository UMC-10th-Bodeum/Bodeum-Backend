-- 출처 검토 결과가 정상으로 확인된 상태임을 명확히 표현한다.
-- 기존 NORMAL 상태를 CONFIRMED_VALID로 변경한다.

UPDATE ai_source_review
SET review_status = 'CONFIRMED_VALID',
    updated_at = NOW(6)
WHERE review_status = 'NORMAL';
