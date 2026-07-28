package com.bodeum.domain.home.repository;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.enums.CommentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomeCommentRepository extends JpaRepository<Comment, Long> {

    @Query("""
            SELECT c.post.id, COUNT(c)
            FROM Comment c
            WHERE c.post.id IN :postIds
              AND c.status = :status
              AND c.deletedAt IS NULL
            GROUP BY c.post.id
            """)
    List<Object[]> countGroupByPostIdIn(
            @Param("postIds") List<Long> postIds,
            @Param("status") CommentStatus status
    );
}
