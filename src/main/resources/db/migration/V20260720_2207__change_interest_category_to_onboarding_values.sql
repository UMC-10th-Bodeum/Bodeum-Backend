-- 온보딩 관심사(user_interests.interest_category) 값을 설계 기준 4개로 변경한다.
--   WELFARE_SUBSIDY / HOSPITAL_HEALTH / PARENTING_COMMUNICATION / GROWTH_EDUCATION
-- 기존 5개(INSTITUTION/HOSPITAL/WELFARE/EMPLOYMENT/EDUCATION) 중 대응되는 3개는 새 값으로 매핑하고,
-- 대응되는 카테고리가 없는 INSTITUTION/EMPLOYMENT 행은 삭제한다. (사용자는 재선택)

DELETE FROM user_interests
WHERE interest_category IN ('INSTITUTION', 'EMPLOYMENT');

UPDATE user_interests
SET interest_category = CASE interest_category
    WHEN 'HOSPITAL'  THEN 'HOSPITAL_HEALTH'
    WHEN 'WELFARE'   THEN 'WELFARE_SUBSIDY'
    WHEN 'EDUCATION' THEN 'GROWTH_EDUCATION'
    ELSE interest_category
END
WHERE interest_category IN ('HOSPITAL', 'WELFARE', 'EDUCATION');
