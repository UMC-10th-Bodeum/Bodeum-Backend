package com.bodeum.domain.info.dto.response;

import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.entity.InfoReviewImage;
import com.bodeum.domain.user.entity.User;
import com.bodeum.global.common.constant.WithdrawalConstants;

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
        boolean isHelpful, // 로그인 유저가 이 후기에 도움돼요를 눌렀는지 여부
        Instant createdAt
) {
    public static InfoReviewResponse from(InfoReview entity) {
        return from(entity, false);
    }

    public static InfoReviewResponse from(InfoReview entity, boolean isHelpful) {
        List<String> imageUrls = entity.getImages().stream()
                .map(InfoReviewImage::getImageUrl)
                .toList();

        User author = entity.getUser();
        boolean withdrawn = author.isWithdrawn();
        Long userId = withdrawn ? null : author.getId();
        String userNickname = withdrawn ? WithdrawalConstants.WITHDRAWN_DISPLAY_NAME : author.getNickname();

        return new InfoReviewResponse(
                entity.getId(),
                userId,
                userNickname,
                entity.getRating(),
                entity.getContent(),
                imageUrls,
                entity.getHelpfulCount(),
                isHelpful,
                entity.getCreatedAt()
        );
    }
}