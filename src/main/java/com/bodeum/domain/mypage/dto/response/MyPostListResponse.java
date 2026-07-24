package com.bodeum.domain.mypage.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public record MyPostListResponse(
        long totalCount,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        List<MyPostItem> posts
) {

    public MyPostListResponse {
        posts = List.copyOf(posts);
    }

    public static MyPostListResponse from(Page<Post> postPage) {
        return new MyPostListResponse(
                postPage.getTotalElements(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalPages(),
                postPage.hasNext(),
                postPage.getContent().stream()
                        .map(MyPostItem::from)
                        .toList()
        );
    }

    public record MyPostItem(
            Long postId,
            PostBoardType boardType,
            PostAnonymityType anonymityType,
            String title,
            String content,
            boolean isQuestion,
            int viewCount,
            int likeCount,
            int commentCount,
            int scrapCount,
            Instant createdAt,
            Instant updatedAt
    ) {

        private static MyPostItem from(Post post) {
            return new MyPostItem(
                    post.getId(),
                    post.getBoardType(),
                    post.getAnonymityType(),
                    post.getTitle(),
                    post.getContent(),
                    post.isQuestion(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getScrapCount(),
                    post.getCreatedAt(),
                    post.getUpdatedAt()
            );
        }
    }
}
