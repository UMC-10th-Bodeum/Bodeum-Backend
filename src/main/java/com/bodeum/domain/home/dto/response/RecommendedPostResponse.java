package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;

import java.time.Instant;

public record RecommendedPostResponse(
        Long postId,
        String categoryName,
        String authorDisplay,
        String title,
        String content,
        long likeCount,
        long commentCount,
        long viewCount,
        Instant createdAt
) {
    public static RecommendedPostResponse from(Post post) {
        return new RecommendedPostResponse(
                post.getId(),
                toBoardTypeName(post.getBoardType()),
                toAuthorDisplay(post.getAnonymityType()),
                post.getTitle(),
                post.getContent(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount(),
                post.getCreatedAt()

        );
    }

    private static String toBoardTypeName(PostBoardType boardType) {
        return switch (boardType) {
            case FREE_COMMUNICATION -> "자유 소통";
            case TREATMENT_GROWTH_RECORD -> "치료·성장 기록";
            case NEIGHBORHOOD_NEWS -> "이웃 소식";
            case INSTITUTION_CENTER_REVIEW -> "기관·센터 후기";
            case INFORMATION_QUESTION -> "정보·질문 광장";
        };
    }

    private static String toAuthorDisplay(PostAnonymityType anonymityType) {
        return anonymityType == PostAnonymityType.FULLY_ANONYMOUS ? "익명 부모님" : "부모님";
    }
}
