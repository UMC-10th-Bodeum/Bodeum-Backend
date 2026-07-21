package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    void deleteAllByComment_Post_Id(Long postId);
}
