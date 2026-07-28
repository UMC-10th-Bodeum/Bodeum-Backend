package com.bodeum.domain.auth.service;

import com.bodeum.domain.user.event.UserPrincipalChangedEvent;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 사용자 변경 이벤트를 받아 인증 캐시·denylist를 무효화한다.
 *
 * <p>커밋 이후(AFTER_COMMIT)에 처리해, 커밋되지 않은 옛 값을 다른 요청이 다시 캐시하는
 * 경쟁을 막는다. 트랜잭션 밖에서 발행된 경우에도 동작하도록 fallbackExecution=true로 둔다.
 */
@Component
@RequiredArgsConstructor
public class UserPrincipalChangedListener {

    private final AuthPrincipalCache authPrincipalCache;
    private final AccessTokenDenylist accessTokenDenylist;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(UserPrincipalChangedEvent event) {
        authPrincipalCache.evict(event.authSubject());
        if (event.revokeAccessTokens()) {
            accessTokenDenylist.revokeAllBefore(event.authSubject(), Instant.now());
        }
    }
}
