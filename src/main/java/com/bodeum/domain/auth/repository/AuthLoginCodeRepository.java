package com.bodeum.domain.auth.repository;

import com.bodeum.domain.auth.entity.AuthLoginCode;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthLoginCodeRepository extends JpaRepository<AuthLoginCode, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select loginCode from AuthLoginCode loginCode where loginCode.code = :code")
    Optional<AuthLoginCode> findByCodeForUpdate(@Param("code") String code);

    @Modifying
    @Query("delete from AuthLoginCode loginCode where loginCode.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
