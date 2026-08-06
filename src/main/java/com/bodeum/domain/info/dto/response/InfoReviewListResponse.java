package com.bodeum.domain.info.dto.response;

import org.springframework.data.domain.Page;

public record InfoReviewListResponse(
        double averageRating,       // 전체 평균 평점 (실수형, 소수점 첫째자리 반올림)
        long totalElements,         // 전체 리뷰 개수
        Page<InfoReviewResponse> reviews // 리뷰 페이징 목록
) {
    public static InfoReviewListResponse of(double averageRating, Page<InfoReviewResponse> reviews) {
        return new InfoReviewListResponse(
                Math.round(averageRating * 10.0) / 10.0,
                reviews.getTotalElements(),
                reviews
        );
    }
}