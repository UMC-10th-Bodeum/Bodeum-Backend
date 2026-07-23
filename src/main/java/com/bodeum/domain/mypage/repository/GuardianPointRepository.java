package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.mypage.entity.GuardianPoint;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianPointRepository extends JpaRepository<GuardianPoint, Long> {

    @Query("select p.id from GuardianPoint p where p.guardianProfileId = :guardianProfileId")
    List<Long> findIdsByGuardianProfileId(@Param("guardianProfileId") Long guardianProfileId);

    @Modifying
    @Query("delete from GuardianPoint p where p.guardianProfileId = :guardianProfileId")
    int deleteByGuardianProfileId(@Param("guardianProfileId") Long guardianProfileId);
}
