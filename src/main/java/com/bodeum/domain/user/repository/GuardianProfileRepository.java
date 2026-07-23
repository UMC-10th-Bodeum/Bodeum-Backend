package com.bodeum.domain.user.repository;

import com.bodeum.domain.user.entity.GuardianProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianProfileRepository extends JpaRepository<GuardianProfile, Long> {

    @Query("select p.id from GuardianProfile p where p.user.id = :userId")
    Optional<Long> findIdByUserId(@Param("userId") Long userId);
}
