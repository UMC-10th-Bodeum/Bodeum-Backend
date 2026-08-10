-- 게시글 작성 화면과 후속 기능에서 사용하지 않는 태그 모델을 제거한다.
-- post_hashtag가 hashtag를 참조하므로 연결 테이블을 먼저 삭제해야 한다.
DROP TABLE IF EXISTS post_hashtag;
DROP TABLE IF EXISTS hashtag;
DROP TABLE IF EXISTS community_post_disability_tag;
