package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByComment_IdAndUserId(Long commentId, Long userId);

    Optional<CommentLike> findByComment_IdAndUserId(Long commentId, Long userId);

    // 회원 탈퇴 시: 해당 회원이 공감한 댓글·답글의 likeCount를 1 감소시킨다.
    // 유니크 제약(user_id, comment_id)으로 댓글당 공감은 최대 1건이므로 정확히 1씩 감소한다.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Comment c SET c.likeCount = c.likeCount - 1
            WHERE c.likeCount > 0
              AND c.id IN (SELECT cl.comment.id FROM CommentLike cl WHERE cl.userId = :userId)
            """)
    int decreaseLikeCountForUserLikes(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CommentLike cl WHERE cl.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    @Query("""
            select cl.comment.id from CommentLike cl
            where cl.userId = :userId
              and cl.comment.id in :commentIds
            """)
    List<Long> findLikedCommentIds(
            @Param("userId") Long userId,
            @Param("commentIds") List<Long> commentIds
    );

}
