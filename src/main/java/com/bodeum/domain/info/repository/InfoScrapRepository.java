package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfoScrapRepository extends JpaRepository<InfoScrap, Long> {

    // 회원 탈퇴 시: 해당 회원이 스크랩한 정보의 scrapCount를 1 감소시킨다.
    // 유니크 제약(user_id, info_item_id)으로 정보당 스크랩은 최대 1건이므로 정확히 1씩 감소한다.
    // 삭제(deleteByUserId)보다 먼저 호출해야 대상 info_item_id를 조회할 수 있다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE InfoItem i SET i.scrapCount = i.scrapCount - 1
            WHERE i.scrapCount > 0
              AND i.id IN (SELECT s.infoItem.id FROM InfoScrap s WHERE s.user.id = :userId)
            """)
    int decreaseScrapCountForUserScraps(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM InfoScrap s WHERE s.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
