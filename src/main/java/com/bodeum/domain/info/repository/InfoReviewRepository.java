package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoReview;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // 동시성 처리를 위해 PESSIMISTIC_WRITE 락 적용 메서드 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM InfoReview r WHERE r.id = :id")
    Optional<InfoReview> findByIdWithPessimisticLock(@Param("id") Long id);
}