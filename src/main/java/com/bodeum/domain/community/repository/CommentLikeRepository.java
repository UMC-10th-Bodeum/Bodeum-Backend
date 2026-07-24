package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.CommentLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    boolean existsByComment_IdAndUserId(Long commentId, Long userId);

    Optional<CommentLike> findByComment_IdAndUserId(Long commentId, Long userId);

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
