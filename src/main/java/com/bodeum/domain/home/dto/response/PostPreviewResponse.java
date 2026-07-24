package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostBoardType;

public record PostPreviewResponse(
        Long postId,
        String categoryName,
        String regionName,
        String title,
        long likeCount,
        long commentCount,
        long viewCount
) {
    public static PostPreviewResponse of(Post post, String regionName) {
        return new PostPreviewResponse(
                post.getId(),
                toBoardTypeName(post.getBoardType()),
                regionName,
                post.getTitle(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getViewCount()
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
}
