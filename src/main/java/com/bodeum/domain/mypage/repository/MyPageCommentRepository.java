package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyPageCommentRepository
        extends JpaRepository<Comment, Long> {

    @Query("""
            select count(comment)
            from Comment comment
            where comment.userId = :userId
              and comment.status = :commentStatus
              and comment.deletedAt is null
              and comment.post.status = :postStatus
              and comment.post.deletedAt is null
            """)
    long countVisibleByUserId(
            @Param("userId") Long userId,
            @Param("commentStatus") CommentStatus commentStatus,
            @Param("postStatus") PostStatus postStatus
    );
}
