package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.PostStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyPagePostScrapRepository
        extends JpaRepository<PostScrap, Long> {

    @Query("""
            select count(scrap)
            from PostScrap scrap
            where scrap.userId = :userId
              and scrap.post.status = :status
              and scrap.post.deletedAt is null
            """)
    long countVisibleByUserId(
            @Param("userId") Long userId,
            @Param("status") PostStatus status
    );

    @Query("""
            select scrap
            from PostScrap scrap
            join fetch scrap.post post
            where scrap.userId = :userId
              and post.status = :status
              and post.deletedAt is null
            order by scrap.createdAt desc
            """)
    List<PostScrap> findAllVisibleByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("status") PostStatus status
    );

    @Query("""
            select scrap
            from PostScrap scrap
            join fetch scrap.post post
            where scrap.userId = :userId
              and post.status = :status
              and post.deletedAt is null
            order by scrap.createdAt desc
            """)
    List<PostScrap> findRecentVisibleByUserId(
            @Param("userId") Long userId,
            @Param("status") PostStatus status,
            Pageable pageable
    );
}
