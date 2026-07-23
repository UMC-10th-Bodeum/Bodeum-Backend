package com.bodeum.domain.home.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;

import java.time.Instant;
import java.util.List;

public record RecommendedPostResponse(
        Long postId,
        List<DisabilityTagDto> disabilityTags,
        String categoryName,
        String authorDisplay,
        String title,
        String content,
        long likeCount,
        long commentCount,
        long viewCount,
        Instant createdAt
) {
    public record DisabilityTagDto(String code, String label) {
        public static DisabilityTagDto from(DisabilityType type) {
            return new DisabilityTagDto(type.name(), type.getLabel());
        }
    }

    public static RecommendedPostResponse of(Post post, List<DisabilityType> disabilityTypes) {
        return new RecommendedPostResponse(
                post.getId(),
                disabilityTypes.stream().map(DisabilityTagDto::from).toList(),
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
