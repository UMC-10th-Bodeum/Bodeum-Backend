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
        Instant createdAt
) {
    public static InfoReviewResponse from(InfoReview entity) {
        List<String> imageUrls = entity.getImages().stream()
                .map(InfoReviewImage::getImageUrl)
                .toList();

        // 리뷰는 보존하되, 작성자가 탈퇴 회원이면 userId를 숨기고 '탈퇴한 사용자'로 익명화한다.
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
                entity.getCreatedAt()
        );
    }
}
