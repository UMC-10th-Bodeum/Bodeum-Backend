package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bodeum.domain.community.dto.request.CreateCommentRequest;
import com.bodeum.domain.community.dto.request.UpdateCommentRequest;
import com.bodeum.domain.community.dto.response.CommentListResponse;
import com.bodeum.domain.community.dto.response.CommentResponse;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.CommentLikeRepository;
import com.bodeum.domain.community.repository.CommentRepository;
import com.bodeum.domain.community.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@Transactional
class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Test
    void createsAndReadsCommentsWithUnlimitedNestedReplies() {
        Post post = savePost();
        CommentResponse root = commentService.createComment(
                20L,
                post.getId(),
                new CreateCommentRequest("댓글")
        );
        CommentResponse reply = commentService.createReply(
                21L,
                root.commentId(),
                new CreateCommentRequest("답글")
        );
        CommentResponse nestedReply = commentService.createReply(
                22L,
                reply.commentId(),
                new CreateCommentRequest("답글에 대한 답글")
        );

        CommentListResponse response = commentService.getComments(22L, post.getId());

        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().commentId()).isEqualTo(root.commentId());
        assertThat(response.comments().getFirst().replies().getFirst().commentId())
                .isEqualTo(reply.commentId());
        assertThat(response.comments().getFirst().replies().getFirst().replies().getFirst().commentId())
                .isEqualTo(nestedReply.commentId());
        assertThat(response.comments().getFirst().replies().getFirst().replies().getFirst().isMine())
                .isTrue();
        assertThat(postRepository.findById(post.getId()).orElseThrow().getCommentCount()).isEqualTo(3);
    }

    @Test
    void updateReturnsModifiedAtAndDeleteExcludesCommentWhileKeepingActiveReply() {
        Post post = savePost();
        CommentResponse root = commentService.createComment(
                20L,
                post.getId(),
                new CreateCommentRequest("원본 댓글")
        );
        CommentResponse reply = commentService.createReply(
                21L,
                root.commentId(),
                new CreateCommentRequest("유지할 답글")
        );

        assertThatThrownBy(() -> commentService.updateComment(
                30L,
                root.commentId(),
                new UpdateCommentRequest("권한 없는 수정")
        ))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.COMMENT_FORBIDDEN);

        CommentResponse updated = commentService.updateComment(
                20L,
                root.commentId(),
                new UpdateCommentRequest("수정된 댓글")
        );
        var persistedUpdatedAt = commentRepository.findById(root.commentId()).orElseThrow().getUpdatedAt();

        assertThat(updated.content()).isEqualTo("수정된 댓글");
        assertThat(updated.updatedAt()).isNotNull();
        assertThat(updated.updatedAt()).isEqualTo(persistedUpdatedAt);

        commentService.deleteComment(20L, root.commentId());

        CommentListResponse response = commentService.getComments(21L, post.getId());

        assertThat(response.totalCount()).isOne();
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().commentId()).isEqualTo(reply.commentId());
        assertThat(response.comments().getFirst().parentCommentId()).isNull();
        assertThat(response.comments().getFirst().content()).isEqualTo("유지할 답글");
    }

    @Test
    void commentLikesAreIdempotentAndReflectedInCommentList() {
        Post post = savePost();
        CommentResponse comment = commentService.createComment(
                20L,
                post.getId(),
                new CreateCommentRequest("좋아요 대상 댓글")
        );

        commentService.likeComment(30L, comment.commentId());
        commentService.likeComment(30L, comment.commentId());

        CommentListResponse liked = commentService.getComments(30L, post.getId());

        assertThat(commentLikeRepository.count()).isOne();
        assertThat(liked.comments().getFirst().isLiked()).isTrue();
        assertThat(liked.comments().getFirst().likeCount()).isOne();

        commentService.unlikeComment(30L, comment.commentId());
        commentService.unlikeComment(30L, comment.commentId());

        CommentListResponse unliked = commentService.getComments(30L, post.getId());

        assertThat(commentLikeRepository.count()).isZero();
        assertThat(unliked.comments().getFirst().isLiked()).isFalse();
        assertThat(unliked.comments().getFirst().likeCount()).isZero();
    }

    @Test
    void cannotReplyToDeletedComment() {
        Post post = savePost();
        CommentResponse comment = commentService.createComment(
                20L,
                post.getId(),
                new CreateCommentRequest("삭제할 댓글")
        );
        commentService.deleteComment(20L, comment.commentId());
        commentRepository.flush();

        assertThatThrownBy(() -> commentService.createReply(
                30L,
                comment.commentId(),
                new CreateCommentRequest("등록할 수 없는 답글")
        ))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.COMMENT_NOT_FOUND);
    }

    private Post savePost() {
        return postRepository.saveAndFlush(Post.create(
                10L,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "댓글 테스트 게시글",
                "댓글과 중첩 답글을 테스트합니다.",
                false
        ));
    }
}
