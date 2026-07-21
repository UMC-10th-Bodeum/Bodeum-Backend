package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InfoReviewRepository extends JpaRepository<InfoReview, Long> {

    // 특정 정보의 리뷰 목록 페이징 조회 (작성자 Fetch Join)
    @Query("SELECT DISTINCT r FROM InfoReview r " +
            "JOIN FETCH r.user " +
            "WHERE r.infoItem.id = :infoItemId")
    Page<InfoReview> findByInfoItemId(@Param("infoItemId") Long infoItemId, Pageable pageable);

    // 단건 조회 시 작성자 및 첨부 이미지 Fetch Join
    @Query("SELECT r FROM InfoReview r " +
            "JOIN FETCH r.user " +
            "LEFT JOIN FETCH r.images " +
            "WHERE r.id = :infoReviewId")
    Optional<InfoReview> findByIdWithUserAndImages(@Param("infoReviewId") Long infoReviewId);
}