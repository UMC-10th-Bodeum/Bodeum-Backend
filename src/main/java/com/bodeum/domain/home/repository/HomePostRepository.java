package com.bodeum.domain.home.repository;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HomePostRepository extends JpaRepository<Post, Long> {

    @Query("""
            SELECT p FROM Post p
            WHERE p.status = :status
              AND p.deletedAt IS NULL
            ORDER BY p.likeCount DESC, p.createdAt DESC
            """)
    List<Post> findTopByPopularity(@Param("status") PostStatus status, Pageable pageable);

    List<Post> findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
            PostStatus status,
            Pageable pageable
    );
}
