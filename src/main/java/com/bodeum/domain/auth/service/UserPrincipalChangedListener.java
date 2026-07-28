package com.bodeum.domain.auth.service;

import com.bodeum.domain.user.event.UserPrincipalChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사용자 변경 이벤트를 받아 인증 principal 캐시를 무효화한다.
 *
 * <p>커밋 이후(AFTER_COMMIT)에 처리해, 커밋되지 않은 옛 값을 다른 요청이 다시 캐시하는
 * 경쟁을 막는다. 트랜잭션 밖에서 발행된 경우에도 동작하도록 fallbackExecution=true로 둔다.
 *
 * <p>캐시 evict는 best-effort다(실패해도 Redis 장애 시 조회가 캐시 미스→DB 폴백으로 처리됨).
 * 탈퇴처럼 실패를 삼키면 안 되는 access token denylist 등록은 이 리스너가 아니라
 * 발행 측에서 커밋 전에 strict로 수행한다.
 */
@Component
@RequiredArgsConstructor
public class UserPrincipalChangedListener {

    private final AuthPrincipalCache authPrincipalCache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserPrincipalChangedEvent event) {
        authPrincipalCache.evict(event.authSubject());
    }
}
