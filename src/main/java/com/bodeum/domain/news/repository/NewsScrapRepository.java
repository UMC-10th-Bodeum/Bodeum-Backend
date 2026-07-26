package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.NewsScrap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {

    boolean existsByNewsIdAndUserId(Long newsId, Long userId);

    Optional<NewsScrap> findByNewsIdAndUserId(Long newsId, Long userId);

    // 회원 탈퇴 시: 해당 회원이 스크랩한 뉴스의 scrapCount를 1 감소시킨다.
    // 유니크 제약(user_id, news_id)으로 뉴스당 스크랩은 최대 1건이므로 정확히 1씩 감소한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE News n SET n.scrapCount = n.scrapCount - 1
            WHERE n.scrapCount > 0
              AND n.id IN (SELECT s.news.id FROM NewsScrap s WHERE s.userId = :userId)
            """)
    int decreaseScrapCountForUserScraps(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM NewsScrap s WHERE s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
