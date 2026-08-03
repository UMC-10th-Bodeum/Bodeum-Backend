package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.entity.InfoReviewHelpful;
import com.bodeum.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InfoReviewHelpfulRepository extends JpaRepository<InfoReviewHelpful, Long> {

    Optional<InfoReviewHelpful> findByUserAndInfoReview(User user, InfoReview infoReview);

    boolean existsByUserAndInfoReview(User user, InfoReview infoReview);

    @Query("SELECT h.infoReview.id FROM InfoReviewHelpful h WHERE h.user.id = :userId AND h.infoReview.id IN :reviewIds")
    Set<Long> findInfoReviewIdsByUserIdAndInfoReviewIdIn(@Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
}