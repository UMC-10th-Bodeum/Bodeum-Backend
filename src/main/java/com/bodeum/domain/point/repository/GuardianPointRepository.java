package com.bodeum.domain.point.repository;

import com.bodeum.domain.point.entity.GuardianPoint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianPointRepository extends JpaRepository<GuardianPoint, Long> {

    @Query("""
            SELECT guardianPoint
            FROM GuardianPoint guardianPoint
            WHERE guardianPoint.guardianProfileId = (
                SELECT guardianProfile.id
                FROM GuardianProfile guardianProfile
                WHERE guardianProfile.user.id = :userId
            )
            """)
    Optional<GuardianPoint> findByUserId(@Param("userId") Long userId);
}
