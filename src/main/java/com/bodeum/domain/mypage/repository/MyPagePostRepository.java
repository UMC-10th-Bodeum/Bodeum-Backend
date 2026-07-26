package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyPagePostRepository
        extends JpaRepository<Post, Long> {

    @Query("""
            select count(post)
            from Post post
            where post.userId = :userId
              and post.status = :status
              and post.deletedAt is null
            """)
    long countVisibleByUserId(
            @Param("userId") Long userId,
            @Param("status") PostStatus status
    );

    @Query(
            value = """
                    select post
                    from Post post
                    where post.userId = :userId
                      and post.status = :status
                      and post.deletedAt is null
                    order by post.createdAt desc
                    """,
            countQuery = """
                    select count(post)
                    from Post post
                    where post.userId = :userId
                      and post.status = :status
                      and post.deletedAt is null
                    """
    )
    Page<Post> findAllVisibleByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            Pageable pageable
    );
}
