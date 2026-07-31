package com.bodeum.domain.ai.infrastructure.indexing;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiIndexingCoordinator {

    private static final String LOCK_NAME = "bodeum:ai:indexing";

    private final JdbcTemplate jdbcTemplate;
    private final int lockTimeoutSeconds;
    private final int maxRetries; // 팀원 코드 흐름 유지용 재시도 설정 추가

    public AiIndexingCoordinator(
            JdbcTemplate jdbcTemplate,
            @Value("${bodeum.ai.indexing.lock-timeout-seconds:60}") int lockTimeoutSeconds, // 디폴트 타임아웃을 30s -> 60s로 상향
            @Value("${bodeum.ai.indexing.max-retries:3}") int maxRetries // 락 획득 실패 시 재시도 횟수 (기본 3회)
    ) {
        if (lockTimeoutSeconds < 0) {
            throw new IllegalArgumentException("AI indexing lock timeout must not be negative");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
        this.maxRetries = Math.max(1, maxRetries);
    }

    public <T> T execute(Supplier<T> task) {
        return jdbcTemplate.execute((Connection connection) -> {
            // MySQL named lock으로 전체 재색인과 증분 색인을 직렬화한다.
            // 같은 DB를 사용하는 여러 애플리케이션 인스턴스 사이에서도 동일하게 적용된다.
            acquireWithRetry(connection);
            try {
                return task.get();
            } finally {
                release(connection);
            }
        });
    }

    // 기존 acquire()를 호출하되, 설정된 횟수만큼 안전하게 재시도
    private void acquireWithRetry(Connection connection) throws SQLException {
        int attempts = 0;
        while (true) {
            try {
                attempts++;
                acquire(connection);
                return; // 락 획득 성공 시 즉시 리턴
            } catch (IllegalStateException e) {
                if (attempts >= maxRetries) {
                    throw e; // 설정한 재시도 횟수를 모두 초과하면 기존 예외 그대로 방출
                }
                try {
                    Thread.sleep(1000); // 1초 대기 후 다시 락 획득 시도
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for AI indexing lock", ie);
                }
            }
        }
    }

    private void acquire(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, LOCK_NAME);
            statement.setInt(2, lockTimeoutSeconds);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || result.getInt(1) != 1) {
                    throw new IllegalStateException("Failed to acquire AI indexing lock");
                }
            }
        }
    }

    private void release(Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, LOCK_NAME);
            statement.executeQuery();
        } catch (SQLException ignored) {
            // The DB session releases its named locks when the connection closes.
        }
    }
}