package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostScrap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    Optional<PostScrap> findByPost_IdAndUserId(Long postId, Long userId);

    void deleteAllByPost_Id(Long postId);

    // 회원 탈퇴 시: 해당 회원이 스크랩한 게시글의 scrapCount를 1 감소시킨다.
    // 유니크 제약(user_id, post_id)으로 게시글당 스크랩은 최대 1건이므로 정확히 1씩 감소한다.
    // 삭제(deleteByUserId)보다 먼저 호출해야 대상 post_id를 조회할 수 있다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Post p SET p.scrapCount = p.scrapCount - 1
            WHERE p.scrapCount > 0
              AND p.id IN (SELECT s.post.id FROM PostScrap s WHERE s.userId = :userId)
            """)
    int decreaseScrapCountForUserScraps(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostScrap s WHERE s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
