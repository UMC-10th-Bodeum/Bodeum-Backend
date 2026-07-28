package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostLike;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    Optional<PostLike> findByPost_IdAndUserId(Long postId, Long userId);

    @Query("""
            select postLike.post.id from PostLike postLike
            where postLike.post.id in :postIds
              and postLike.userId = :userId
            """)
    List<Long> findLikedPostIds(
            @Param("postIds") List<Long> postIds,
            @Param("userId") Long userId
    );

    void deleteAllByPost_Id(Long postId);
}
