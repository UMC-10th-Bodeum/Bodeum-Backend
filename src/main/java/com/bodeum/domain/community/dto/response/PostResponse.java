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

        @Schema(example = "10")
        Long authorId,

        PostBoardType boardType,
        PostAnonymityType anonymityType,

        @Schema(example = "아이와 함께 갈 수 있는 공원을 추천해주세요.")
        String title,

        @Schema(example = "주말에 방문하기 좋은 조용한 공원을 찾고 있습니다.")
        String content,

        List<DisabilityType> disabilityTypes,
        List<String> hashtags,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {

    public static PostResponse of(
            Post post,
            List<DisabilityType> disabilityTypes,
            List<String> hashtags,
            List<String> imageUrls
    ) {
        return new PostResponse(
                post.getId(),
                post.getUserId(),
                post.getBoardType(),
                post.getAnonymityType(),
                post.getTitle(),
                post.getContent(),
                List.copyOf(disabilityTypes),
                List.copyOf(hashtags),
                List.copyOf(imageUrls),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
