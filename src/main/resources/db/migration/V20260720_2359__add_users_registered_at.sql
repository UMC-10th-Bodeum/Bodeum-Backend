-- 회원가입 완료(온보딩 3단계 완료 또는 건너뛰기) 시각.
-- NULL = 가입 진행중, 값 있음 = 정식 회원(전환 시각). created_at(계정 생성=콜백)과 별개다.
ALTER TABLE users
    ADD COLUMN registered_at DATETIME(6) NULL;

-- 기존 회원 백필: 이미 온보딩을 완료했거나 건너뛴 사용자를 정식 회원으로 표시한다.
-- 정확한 확정 시각을 알 수 없으므로 최종 수정 시각(updated_at)을 근사값으로 사용한다.
-- 완료 조건은 엔티티의 isOnboardingResolved() 판정과 동일하게 맞춘다.
UPDATE users u
SET u.registered_at = u.updated_at
WHERE u.registered_at IS NULL
  AND (
        u.onboarding_skipped = TRUE
        OR (
            -- 온보딩 1단계: 아이 프로필(생년월 + 케어 영역 1개 이상)
            EXISTS (
                SELECT 1
                FROM child_profiles cp
                WHERE cp.user_id = u.id
                  AND cp.birth IS NOT NULL
                  AND EXISTS (
                        SELECT 1 FROM child_disabilities cd WHERE cd.child_profile_id = cp.id
                  )
            )
            -- 온보딩 2단계: 관심사 1개 이상
            AND EXISTS (
                SELECT 1 FROM user_interests ui WHERE ui.user_id = u.id
            )
            -- 온보딩 2·3단계: 보호자 프로필(닉네임) + 활동 지역
            AND EXISTS (
                SELECT 1
                FROM guardian_profiles gp
                WHERE gp.user_id = u.id
                  AND gp.nickname IS NOT NULL
                  AND gp.region_id IS NOT NULL
            )
        )
      );
