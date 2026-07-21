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

    public AiIndexingCoordinator(
            JdbcTemplate jdbcTemplate,
            @Value("${bodeum.ai.indexing.lock-timeout-seconds:30}") int lockTimeoutSeconds
    ) {
        if (lockTimeoutSeconds < 0) {
            throw new IllegalArgumentException("AI indexing lock timeout must not be negative");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.lockTimeoutSeconds = lockTimeoutSeconds;
    }

    public <T> T execute(Supplier<T> task) {
        return jdbcTemplate.execute((Connection connection) -> {
            acquire(connection);
            try {
                return task.get();
            } finally {
                release(connection);
            }
        });
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
