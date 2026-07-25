-- 회원 탈퇴 사유(withdrawal_reason) 컬럼 제거 (#102)
--
-- 배경: 탈퇴 사유는 자유 입력이라 개인정보가 포함될 수 있어 원문을 저장하지 않았고,
--   코드에서는 항상 null만 기록해 온 사실상 죽은 컬럼이었다. 탈퇴 사유 입력 자체를
--   폐지하면서 엔티티 매핑을 제거했고, 이에 맞춰 컬럼도 정리한다.
ALTER TABLE users DROP COLUMN withdrawal_reason;
