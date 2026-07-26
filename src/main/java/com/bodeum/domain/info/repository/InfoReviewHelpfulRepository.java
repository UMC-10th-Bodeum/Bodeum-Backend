package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.entity.InfoReviewHelpful;
import com.bodeum.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InfoReviewHelpfulRepository extends JpaRepository<InfoReviewHelpful, Long> {

    Optional<InfoReviewHelpful> findByUserAndInfoReview(User user, InfoReview infoReview);

    boolean existsByUserAndInfoReview(User user, InfoReview infoReview);
}