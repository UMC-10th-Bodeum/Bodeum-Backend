package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findByIdAndStatusAndDeletedAtIsNull(Long id, PostStatus status);

    @Query(
            value = """
                    select p from Post p
                    where p.status = :status
                      and p.deletedAt is null
                      and (
                        :keyword is null
                        or lower(p.title) like lower(concat('%', :keyword, '%')) escape '!'
                        or lower(p.content) like lower(concat('%', :keyword, '%')) escape '!'
                      )
                    """,
            countQuery = """
                    select count(p) from Post p
                    where p.status = :status
                      and p.deletedAt is null
                      and (
                        :keyword is null
                        or lower(p.title) like lower(concat('%', :keyword, '%')) escape '!'
                        or lower(p.content) like lower(concat('%', :keyword, '%')) escape '!'
                      )
                    """
    )
    Page<Post> findActivePosts(
            @Param("status") PostStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Post p
            where p.id = :postId
              and p.status = :status
              and p.deletedAt is null
            """)
    Optional<Post> findByIdAndStatusForUpdate(
            @Param("postId") Long postId,
            @Param("status") PostStatus status
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Post p
            set p.viewCount = p.viewCount + 1
            where p.id = :postId
              and p.status = :status
              and p.deletedAt is null
            """)
    int incrementViewCount(@Param("postId") Long postId, @Param("status") PostStatus status);
}
