package com.bodeum.domain.mypage.dto.response;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostBoardType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public record MyCommentListResponse(
        long totalCount,
        int page,
        int size,
        int totalPages,
        boolean hasNext,
        List<MyCommentItem> comments
) {

    public MyCommentListResponse {
        comments = List.copyOf(comments);
    }

    public static MyCommentListResponse from(Page<Comment> commentPage) {
        return new MyCommentListResponse(
                commentPage.getTotalElements(),
                commentPage.getNumber(),
                commentPage.getSize(),
                commentPage.getTotalPages(),
                commentPage.hasNext(),
                commentPage.getContent().stream()
                        .map(MyCommentItem::from)
                        .toList()
        );
    }

    public record MyCommentItem(
            Long commentId,
            Long parentCommentId,
            String content,
            boolean isAccepted,
            int likeCount,
            Instant createdAt,
            Instant updatedAt,
            Long postId,
            PostBoardType postBoardType,
            String postTitle
    ) {

        private static MyCommentItem from(Comment comment) {
            Post post = comment.getPost();
            Long parentCommentId = comment.getParent() == null
                    ? null
                    : comment.getParent().getId();

            return new MyCommentItem(
                    comment.getId(),
                    parentCommentId,
                    comment.getContent(),
                    comment.isAccepted(),
                    comment.getLikeCount(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt(),
                    post.getId(),
                    post.getBoardType(),
                    post.getTitle()
            );
        }
    }
}
