package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AiRequestGuardTest {

    private final AiMessageRepository repository = mock(AiMessageRepository.class);

    @Test
    void rejectsWhenDailyLimitIsReached() {
        when(repository.countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(10L), eq(SenderType.USER), any(Instant.class), any(Instant.class)))
                .thenReturn(50L);
        AiRequestGuard guard = new AiRequestGuard(repository, 5, 50, "Asia/Seoul");

        assertError(() -> guard.acquire(1L, 10L), AiErrorCode.AI_DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void rejectsConcurrentRequestFromSameUser() {
        AiRequestGuard guard = new AiRequestGuard(repository, 5, 50, "Asia/Seoul");
        AiRequestGuard.Permit permit = guard.acquire(1L, 10L);

        try {
            assertError(() -> guard.acquire(1L, 10L), AiErrorCode.AI_REQUEST_IN_PROGRESS);
        } finally {
            permit.close();
        }
    }

    @Test
    void rejectsSixthRequestWithinOneMinute() {
        AiRequestGuard guard = new AiRequestGuard(repository, 5, 50, "Asia/Seoul");
        for (int request = 0; request < 5; request++) {
            guard.acquire(1L, 10L).close();
        }

        assertError(() -> guard.acquire(1L, 10L), AiErrorCode.AI_RATE_LIMIT_EXCEEDED);
    }

    private void assertError(Runnable action, AiErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(expected);
    }
}
