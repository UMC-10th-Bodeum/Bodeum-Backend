package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bodeum.domain.community.dto.request.CreateCommentRequest;
import com.bodeum.domain.community.dto.request.UpdateCommentRequest;
import com.bodeum.domain.community.dto.response.CommentListResponse;
import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.CommentLike;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.CommentLikeRepository;
import com.bodeum.domain.community.repository.CommentRepository;
import com.bodeum.domain.community.repository.PostRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private CommentLikeRepository commentLikeRepository;
    @Mock
    private PostRepository postRepository;
    @Mock
    private com.bodeum.domain.user.repository.UserRepository userRepository;
    @InjectMocks
    private CommentService commentService;

    @Test
    void createCommentIncreasesPostCommentCount() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 1L);
            return comment;
        });

        var response = commentService.createComment(20L, 1L, new CreateCommentRequest("댓글"));

        assertThat(response.commentId()).isEqualTo(1L);
        assertThat(response.isMine()).isTrue();
        assertThat(post.getCommentCount()).isOne();
    }

    @Test
    void createCommentRejectsMissingAuthenticatedUser() {
        assertThatThrownBy(() -> commentService.createComment(
                null,
                1L,
                new CreateCommentRequest("댓글")
        ))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Test
    void createReplyAllowsReplyToAnotherReply() {
        Post post = post(1L, 10L);
        Comment root = comment(1L, post, 20L, null, "댓글");
        Comment reply = comment(2L, post, 21L, root, "답글");
        given(commentRepository.findActiveByIdForUpdate(2L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(reply));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment nestedReply = invocation.getArgument(0);
            ReflectionTestUtils.setField(nestedReply, "id", 3L);
            return nestedReply;
        });

        var response = commentService.createReply(22L, 2L, new CreateCommentRequest("중첩 답글"));

        assertThat(response.parentCommentId()).isEqualTo(2L);
        assertThat(post.getCommentCount()).isEqualTo(3);
    }

    @Test
    void getCommentsBuildsTreeWithoutDepthLimit() {
        Post post = post(1L, 10L);
        Comment root = comment(1L, post, 20L, null, "댓글");
        Comment reply = comment(2L, post, 21L, root, "답글");
        Comment nestedReply = comment(3L, post, 22L, reply, "중첩 답글");
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.findAllActiveByPostIdWithParent(1L, CommentStatus.ACTIVE))
                .willReturn(List.of(root, reply, nestedReply));
        given(commentLikeRepository.findLikedCommentIds(22L, List.of(1L, 2L, 3L)))
                .willReturn(List.of(3L));

        CommentListResponse response = commentService.getComments(22L, 1L);

        assertThat(response.totalCount()).isEqualTo(3);
        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().replies().getFirst().replies().getFirst().commentId())
                .isEqualTo(3L);
        assertThat(response.comments().getFirst().replies().getFirst().replies().getFirst().isLiked())
                .isTrue();
    }

    @Test
    void getCommentsAllowsAnonymousUserWithoutPersonalizedState() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.findAllActiveByPostIdWithParent(1L, CommentStatus.ACTIVE))
                .willReturn(List.of(comment));

        CommentListResponse response = commentService.getComments(null, 1L);

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().isMine()).isFalse();
        assertThat(response.comments().getFirst().isLiked()).isFalse();
        then(commentLikeRepository).should(never()).findLikedCommentIds(any(), any());
    }

    @Test
    void updateCommentRejectsNonOwner() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.updateComment(
                30L,
                1L,
                new UpdateCommentRequest("수정")
        ))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.COMMENT_FORBIDDEN);
    }

    @Test
    void deleteCommentSoftDeletesOwnedCommentAndDecreasesCount() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        commentService.deleteComment(20L, 1L);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.DELETED);
        assertThat(comment.getDeletedAt()).isNotNull();
        assertThat(post.getCommentCount()).isZero();
    }

    @Test
    void deleteCommentRejectsNonOwner() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.deleteComment(30L, 1L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.COMMENT_FORBIDDEN);

        assertThat(comment.getStatus()).isEqualTo(CommentStatus.ACTIVE);
        assertThat(comment.getDeletedAt()).isNull();
        then(postRepository).shouldHaveNoInteractions();
    }

    @Test
    void likeCommentIsIdempotent() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        comment.increaseLikeCount();
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(commentLikeRepository.existsByComment_IdAndUserId(1L, 30L)).willReturn(true);

        var response = commentService.likeComment(30L, 1L);

        assertThat(response.likeCount()).isOne();
        then(commentLikeRepository).should(never()).save(any(CommentLike.class));
    }

    @Test
    void unlikeCommentDeletesLikeAndDecreasesCount() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        comment.increaseLikeCount();
        CommentLike commentLike = CommentLike.create(comment, 30L);
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(commentLikeRepository.findByComment_IdAndUserId(1L, 30L))
                .willReturn(Optional.of(commentLike));

        var response = commentService.unlikeComment(30L, 1L);

        assertThat(response.likeCount()).isZero();
        then(commentLikeRepository).should().delete(commentLike);
    }

    private Post post(Long postId, Long userId) {
        Post post = Post.create(
                userId,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                false
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    private Comment comment(
            Long commentId,
            Post post,
            Long userId,
            Comment parent,
            String content
    ) {
        Comment comment = parent == null
                ? Comment.create(post, userId, content)
                : Comment.createReply(parent, userId, content);
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }
}
