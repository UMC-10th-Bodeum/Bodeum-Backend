package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    Optional<PostLike> findByPost_IdAndUserId(Long postId, Long userId);

    void deleteAllByPost_Id(Long postId);
}
