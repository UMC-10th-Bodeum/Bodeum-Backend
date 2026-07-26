package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.GuardianLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Objects;

public record PostListItemResponse(
        Long postId,
        PostBoardType boardType,
        PostAnonymityType anonymityType,
        String title,
        String content,
        boolean isQuestion,
        AuthorResponse author,
        @Schema(description = "게시글의 첫 번째 이미지 URL. 이미지가 없으면 null")
        String thumbnailUrl,
        int viewCount,
        int likeCount,
        int commentCount,
        int scrapCount,
        @Schema(description = "현재 로그인 사용자의 좋아요 여부")
        boolean isLiked,
        Instant createdAt
) {

    public static PostListItemResponse of(
            Post post,
            User author,
            Long viewerId,
            String thumbnailUrl,
            boolean liked
    ) {
        return new PostListItemResponse(
                post.getId(),
                post.getBoardType(),
                post.getAnonymityType(),
                post.getTitle(),
                post.getContent(),
                post.isQuestion(),
                AuthorResponse.of(post, author, viewerId),
                thumbnailUrl,
                post.getViewCount(),
                post.getLikeCount(),
                post.getCommentCount(),
                post.getScrapCount(),
                liked,
                post.getCreatedAt()
        );
    }

    public record AuthorResponse(
            @Schema(description = "작성자 ID. 완전 익명 게시글이면 null")
            Long authorId,
            String nickname,
            String profileImageUrl,
            Integer level,
            String badgeName,
            boolean isMine
    ) {

        private static AuthorResponse of(Post post, User author, Long viewerId) {
            boolean mine = Objects.equals(post.getUserId(), viewerId);
            if (post.getAnonymityType() == PostAnonymityType.FULLY_ANONYMOUS) {
                return new AuthorResponse(null, "익명", null, null, null, mine);
            }
            if (author == null) {
                return new AuthorResponse(post.getUserId(), null, null, null, null, mine);
            }

            GuardianLevel guardianLevel = author.getGuardianLevel();
            return new AuthorResponse(
                    author.getId(),
                    author.getNickname(),
                    author.getProfileImageUrl(),
                    guardianLevel.getLevelNumber(),
                    guardianLevel.getBadgeName(),
                    mine
            );
        }
    }
}
