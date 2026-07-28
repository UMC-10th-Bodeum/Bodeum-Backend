package com.bodeum.domain.user.event;

/**
 * 사용자의 인증 principal 캐시를 무효화해야 하는 변경(닉네임 등 수정, 탈퇴)이 일어났음을 알리는 이벤트.
 * 캐시 무효화를 개별 서비스에 흩어놓지 않고 한 리스너(AFTER_COMMIT)로 중앙화하기 위해 사용한다.
 *
 * <p>access token denylist 등록처럼 <b>실패를 삼키면 안 되는 보안 무효화</b>는 이 이벤트에 싣지 않는다.
 * (best-effort AFTER_COMMIT로 처리하면 실패가 조용히 사라지므로) 그런 무효화는 커밋 전에 strict로 직접 호출한다.
 *
 * @param authSubject 무효화 대상 사용자의 authSubject(탈퇴처럼 값이 교체되는 경우 교체 전 값)
 */
public record UserPrincipalChangedEvent(String authSubject) {
}
