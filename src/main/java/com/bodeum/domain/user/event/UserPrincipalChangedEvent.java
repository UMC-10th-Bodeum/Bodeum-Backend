package com.bodeum.domain.user.event;

/**
 * 사용자의 인증 principal에 영향을 주는 변경(닉네임 등 수정, 탈퇴)이 일어났음을 알리는 이벤트.
 * 인증 캐시 무효화를 개별 서비스에 흩어놓지 않고 한 리스너로 중앙화하기 위해 사용한다.
 *
 * @param authSubject         무효화 대상 사용자의 authSubject(탈퇴처럼 값이 교체되는 경우 교체 전 값)
 * @param revokeAccessTokens  이미 발급된 access token까지 즉시 폐기해야 하는지(탈퇴 시 true)
 */
public record UserPrincipalChangedEvent(String authSubject, boolean revokeAccessTokens) {
}
