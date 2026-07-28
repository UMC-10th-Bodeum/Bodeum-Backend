-- 소셜 로그인 콜백 → 프론트 리다이렉트 시 발급하는 일회용 로그인 code 저장소.
-- code는 60초 TTL·1회 소비. 프론트가 POST /api/v1/auth/exchange로 교환하면 즉시 삭제된다.
-- 토큰은 저장하지 않고 교환 시점에 발급한다(민감정보 미저장).
-- user_id는 참조값이라 FK를 걸지 않는다(oauth_states와 동일 정책).
CREATE TABLE auth_login_codes (
    code        VARCHAR(64) NOT NULL,
    user_id     BIGINT      NOT NULL,
    is_new_user BOOLEAN     NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (code)
);

-- 만료분 정리 쿼리(deleteExpired: WHERE expires_at <= now)의 풀스캔을 피하기 위한 인덱스.
CREATE INDEX idx_auth_login_codes_expires_at ON auth_login_codes (expires_at);
