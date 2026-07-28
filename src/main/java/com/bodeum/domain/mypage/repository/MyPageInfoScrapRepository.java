package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.info.entity.InfoScrap;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyPageInfoScrapRepository
        extends JpaRepository<InfoScrap, Long> {

    @Query("""
            select count(scrap)
            from InfoScrap scrap
            where scrap.user.id = :userId
            """)
    long countByUserId(
            @Param("userId") Long userId
    );

    @Query("""
            select scrap
            from InfoScrap scrap
            join fetch scrap.infoItem infoItem
            join fetch infoItem.infoCategory infoCategory
            where scrap.user.id = :userId
            order by scrap.createdAt desc
            """)
    List<InfoScrap> findAllByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId
    );
}