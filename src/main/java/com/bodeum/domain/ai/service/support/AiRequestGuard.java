package com.bodeum.domain.ai.service.support;

import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 사용자별 AI 요청의 분당·일일 한도를 검사하고,
 * 동시에 처리되는 요청 수를 안전하게 관리한다.
 */
@Component
public class AiRequestGuard {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final AiMessageRepository aiMessageRepository;
    private final int perMinuteLimit;
    private final int dailyLimit;
    private final ZoneId dailyLimitZone;
    private final Map<Long, RequestState> states = new ConcurrentHashMap<>();

    public AiRequestGuard(
            AiMessageRepository aiMessageRepository,
            @Value("${bodeum.ai.request-limit.per-minute:5}") int perMinuteLimit,
            @Value("${bodeum.ai.request-limit.per-day:50}") int dailyLimit,
            @Value("${bodeum.ai.request-limit.time-zone:Asia/Seoul}") String timeZone
    ) {
        this.aiMessageRepository = aiMessageRepository;
        this.perMinuteLimit = perMinuteLimit;
        this.dailyLimit = dailyLimit;
        this.dailyLimitZone = ZoneId.of(timeZone);
    }

    /**
     * AI 요청 가능 여부를 검증하고 요청 권한을 획득한다.
     *
     * 일일 요청 제한, 1분 요청 제한, 중복 요청 여부를 검사하며,
     * 검증을 통과하면 현재 요청을 진행 중 상태로 표시한다.
     *
     * 반환된 Permit은 요청 종료 시 반드시 close 되어야 한다.
     */
    public Permit acquire(Long userId, Long chatRoomId) {
        Instant now = Instant.now();
        validateDailyLimit(chatRoomId, now);

        RequestState state;
        while (true) {
            // 사용자별 요청 상태가 없으면 새로 생성
            state = states.computeIfAbsent(userId, ignored -> new RequestState());
            synchronized (state) {
                // cleanup 과정에서 이미 제거 대상으로 확정된 객체라면
                // 새 상태를 다시 조회하도록 반복
                if (state.removed) {
                    continue;
                }
                removeExpiredRequests(state, now);
                if (state.inProgress) {
                    throw new ProjectException(AiErrorCode.AI_REQUEST_IN_PROGRESS);
                }
                if (state.acceptedAt.size() >= perMinuteLimit) {
                    throw new ProjectException(AiErrorCode.AI_RATE_LIMIT_EXCEEDED);
                }
                state.acceptedAt.addLast(now);
                state.inProgress = true;
                state.lastAccessedAt = now;
                break;
            }
        }

        // 사용자 상태 Map이 너무 커지는 것을 방지하기 위해 오래된 상태 정리
        cleanupInactiveStates(now);

        RequestState acquiredState = state;

        // try-with-resources 종료 시 release가 호출되도록 Permit 반환
        return () -> release(acquiredState);
    }

    /**
     * 현재 날짜 기준으로 하루 AI 질문 횟수를 검사한다.
     */
    private void validateDailyLimit(Long chatRoomId, Instant now) {
        LocalDate today = now.atZone(dailyLimitZone).toLocalDate();
        Instant startAt = today.atStartOfDay(dailyLimitZone).toInstant();
        Instant endAt = today.plusDays(1).atStartOfDay(dailyLimitZone).toInstant();
        long used = aiMessageRepository
                .countByChatRoomIdAndSenderTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        chatRoomId, SenderType.USER, startAt, endAt);
        if (used >= dailyLimit) {
            throw new ProjectException(AiErrorCode.AI_DAILY_LIMIT_EXCEEDED);
        }
    }

    /**
     * AI 요청 처리가 종료되었음을 표시한다.
     *
     * Permit.close()에서 호출되며,
     * 이후 사용자가 새로운 AI 요청을 보낼 수 있도록 inProgress를 해제한다.
     */
    private void release(RequestState state) {
        synchronized (state) {
            state.inProgress = false;
            state.lastAccessedAt = Instant.now();
        }
    }

    /**
     * 최근 1분 범위를 벗어난 요청 기록을 제거한다.
     *
     * acceptedAt은 시간순으로 저장되므로
     * 앞에서부터 만료된 기록만 제거하면 된다.
     */
    private void removeExpiredRequests(RequestState state, Instant now) {
        Instant cutoff = now.minus(ONE_MINUTE);
        while (!state.acceptedAt.isEmpty() && !state.acceptedAt.getFirst().isAfter(cutoff)) {
            state.acceptedAt.removeFirst();
        }
    }

    /**
     * 오래 사용되지 않은 사용자 요청 상태를 Map에서 제거한다.
     *
     * 사용자 상태가 1,000개 미만이면 정리를 생략하고,
     * 진행 중인 요청이 없으면서 1분 이상 접근되지 않은 상태만 제거한다.
     */
    private void cleanupInactiveStates(Instant now) {
        if (states.size() < 1_000) {
            return;
        }
        Instant cutoff = now.minus(ONE_MINUTE);
        states.entrySet().removeIf(entry -> {
            RequestState state = entry.getValue();
            synchronized (state) {
                if (!state.inProgress && state.lastAccessedAt.isBefore(cutoff)) {

                    // acquire가 제거 예정 객체를 다시 사용하지 않도록 표시
                    state.removed = true;
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * 사용자별 요청 제한 상태.
     */
    private static final class RequestState {

        // 최근 1분 동안 허용된 요청 시각 목록
        private final ArrayDeque<Instant> acceptedAt = new ArrayDeque<>();

        // 현재 AI 요청이 처리 중인지 여부
        private boolean inProgress;

        // cleanup 과정에서 Map 제거 대상으로 확정되었는지 여부
        private boolean removed;

        // 마지막 접근 시각
        private Instant lastAccessedAt = Instant.EPOCH;
    }

    /**
     * AI 요청 사용 권한.
     *
     * try-with-resources와 함께 사용하여
     * 요청 처리 성공/실패 여부와 관계없이 진행 중 상태를 해제한다.
     */
    @FunctionalInterface
    public interface Permit extends AutoCloseable {

        @Override
        void close();
    }
}
