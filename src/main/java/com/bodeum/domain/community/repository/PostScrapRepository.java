package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostScrap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    boolean existsByPost_IdAndUserId(Long postId, Long userId);

    Optional<PostScrap> findByPost_IdAndUserId(Long postId, Long userId);

    void deleteAllByPost_Id(Long postId);

    @Modifying
    @Query("delete from PostScrap s where s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
