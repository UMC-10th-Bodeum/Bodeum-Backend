package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            select c from Comment c
            left join fetch c.parent
            where c.post.id = :postId
              and c.status = :status
              and c.deletedAt is null
            order by c.id asc
            """)
    List<Comment> findAllActiveByPostIdWithParent(
            @Param("postId") Long postId,
            @Param("status") CommentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c from Comment c
            where c.id = :commentId
              and c.status = :commentStatus
              and c.deletedAt is null
              and c.post.status = :postStatus
              and c.post.deletedAt is null
            """)
    Optional<Comment> findActiveByIdForUpdate(
            @Param("commentId") Long commentId,
            @Param("commentStatus") CommentStatus commentStatus,
            @Param("postStatus") PostStatus postStatus
    );

}
