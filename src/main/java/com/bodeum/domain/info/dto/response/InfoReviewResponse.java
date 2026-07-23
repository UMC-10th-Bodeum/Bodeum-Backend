package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.entity.InfoReviewImage;
import com.bodeum.domain.user.entity.User;

import java.time.Instant;
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
    private static final String WITHDRAWN_AUTHOR_NAME = "탈퇴한 사용자";

    public static InfoReviewResponse from(InfoReview entity) {
        List<String> imageUrls = entity.getImages().stream()
                .map(InfoReviewImage::getImageUrl)
                .toList();

        User author = entity.getUser();

        return new InfoReviewResponse(
                entity.getId(),
                author.getId(),
                author.isWithdrawn() ? WITHDRAWN_AUTHOR_NAME : author.getNickname(),
                entity.getRating(),
                entity.getContent(),
                imageUrls,
                entity.getHelpfulCount(),
                entity.getCreatedAt()
        );
    }
}