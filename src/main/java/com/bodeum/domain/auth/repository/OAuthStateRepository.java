package com.bodeum.domain.auth.repository;

import com.bodeum.domain.auth.model.OAuthState;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OAuthStateRepository extends JpaRepository<OAuthState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from OAuthState state where state.state = :state")
    Optional<OAuthState> findByStateForUpdate(@Param("state") String state);

    @Modifying
    @Query("delete from OAuthState state where state.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
