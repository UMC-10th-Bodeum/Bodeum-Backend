package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.entity.InfoReviewImage;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record InfoReviewResponse(
        Long infoReviewId,
        Long userId,
        String userNickname,
        int rating,
        String content,
        List<String> imageUrls,
        int helpfulCount,
        Instant createdAt
) {
    public static InfoReviewResponse from(InfoReview entity) {
        List<String> imageUrls = entity.getImages().stream()
                .map(InfoReviewImage::getImageUrl)
                .toList();

        return new InfoReviewResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getUser().getNickname(),
                entity.getRating(),
                entity.getContent(),
                imageUrls,
                entity.getHelpfulCount(),
                entity.getCreatedAt()
        );
    }
}