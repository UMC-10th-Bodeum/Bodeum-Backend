package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteAllByPost_IdAndParentIsNotNull(Long postId);

    void deleteAllByPost_IdAndParentIsNull(Long postId);
}
