-- OAuth state에 프론트 콜백 URL 보관 컬럼 추가
--
-- 배경: 로그인 완료 후 돌아갈 프론트 주소가 서버 전역 설정(bodeum.front.callback-url) 하나뿐이라,
--   프론트 로컬 개발자가 붙을 때마다 EC2의 FRONT_CALLBACK_URL을 바꿔야 했고 그동안 운영 로그인이 깨졌다.
--   로그인 시작 시 프론트가 지정한 콜백 URL을 state 행에 실어 콜백까지 왕복시키면
--   운영/로컬이 같은 서버에서 동시에 동작한다. state는 서버가 발급·저장하므로 위변조가 불가능하다.
--
-- 배포 순서: nullable 컬럼 추가라 구버전 앱(이 컬럼을 매핑하지 않는 이미지)이 함께 떠 있어도 안전하다.
--   Hibernate ddl-auto=validate는 매핑된 컬럼의 존재만 확인하고 여분 컬럼은 무시하므로 롤백도 안전하다.
ALTER TABLE oauth_states ADD COLUMN front_callback_url VARCHAR(255) NULL;
