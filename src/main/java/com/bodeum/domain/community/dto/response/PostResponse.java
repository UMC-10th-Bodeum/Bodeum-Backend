package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record PostResponse(
        @Schema(example = "1")
        Long postId,

        @Schema(
                description = "작성자 ID. 완전 익명 게시글이면 null",
                example = "10",
                nullable = true
        )
        Long authorId,

        @Schema(description = "현재 로그인 사용자의 게시글 여부", example = "true")
        boolean isMine,

        PostBoardType boardType,
        PostAnonymityType anonymityType,

        @Schema(example = "아이와 함께 갈 수 있는 공원을 추천해주세요.")
        String title,

        @Schema(example = "주말에 방문하기 좋은 조용한 공원을 찾고 있습니다.")
        String content,

        @Schema(description = "질문글 여부", example = "true")
        boolean isQuestion,

        int viewCount,
        int likeCount,
        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        boolean isLiked,
        int commentCount,
        int scrapCount,
        @Schema(description = "현재 사용자의 스크랩 여부", example = "false")
        boolean isScrapped,

        List<DisabilityType> disabilityTypes,
        List<String> hashtags,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {

    public static PostResponse of(
            Post post,
            Long viewerId,
            boolean liked,
            boolean scrapped,
            List<DisabilityType> disabilityTypes,
            List<String> hashtags,
            List<String> imageUrls
    ) {
        return new PostResponse(
                post.getId(),
                post.getAnonymityType() == PostAnonymityType.FULLY_ANONYMOUS ? null : post.getUserId(),
                post.getUserId().equals(viewerId),
                post.getBoardType(),
                post.getAnonymityType(),
                post.getTitle(),
                post.getContent(),
                post.isQuestion(),
                post.getViewCount(),
                post.getLikeCount(),
                liked,
                post.getCommentCount(),
                post.getScrapCount(),
                scrapped,
                List.copyOf(disabilityTypes),
                List.copyOf(hashtags),
                List.copyOf(imageUrls),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
