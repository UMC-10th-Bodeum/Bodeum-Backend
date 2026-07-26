package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    Optional<PostLike> findByPost_IdAndUserId(Long postId, Long userId);

    void deleteAllByPost_Id(Long postId);

    // 회원 탈퇴 시: 해당 회원이 좋아요한 게시글의 likeCount를 1 감소시킨다.
    // 유니크 제약(user_id, post_id)으로 게시글당 좋아요는 최대 1건이므로 정확히 1씩 감소한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Post p SET p.likeCount = p.likeCount - 1
            WHERE p.likeCount > 0
              AND p.id IN (SELECT l.post.id FROM PostLike l WHERE l.userId = :userId)
            """)
    int decreaseLikeCountForUserLikes(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM PostLike l WHERE l.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
