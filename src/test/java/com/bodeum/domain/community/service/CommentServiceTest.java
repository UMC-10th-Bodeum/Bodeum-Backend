package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bodeum.domain.auth.enums.SocialProvider;
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
import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
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
    private UserRepository userRepository;
    @Mock
    private PointService pointService;
    @InjectMocks
    private CommentService commentService;

    @Test
    void createCommentOnGeneralPostGrantsAnswerPoint() {
        Post post = post(1L, 10L);
        User author = user(20L, "보듬맘");
        ReflectionTestUtils.setField(
                author,
                "profileImageUrl",
                "https://example.com/profile.jpg"
        );
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 1L);
            return comment;
        });
        given(userRepository.findAllById(any())).willReturn(List.of(author));

        var response = commentService.createComment(20L, 1L, new CreateCommentRequest("댓글"));

        assertThat(response.commentId()).isEqualTo(1L);
        assertThat(response.isMine()).isTrue();
        assertThat(response.profileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(post.getCommentCount()).isOne();
        then(pointService).should().grantActivityPoint(
                20L,
                PointEventType.COMMUNITY_ANSWER_CREATED,
                1L,
                20L
        );
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
    void createCommentOnQuestionPostGrantsAnswerPoint() {
        Post post = post(1L, 10L, PostBoardType.INFORMATION_QUESTION);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.save(any(Comment.class))).willAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            ReflectionTestUtils.setField(comment, "id", 2L);
            return comment;
        });

        commentService.createComment(20L, 1L, new CreateCommentRequest("답변"));

        then(pointService).should().grantActivityPoint(
                20L,
                PointEventType.COMMUNITY_ANSWER_CREATED,
                2L,
                20L
        );
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
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void getCommentsBuildsTreeWithoutDepthLimit() {
        Post post = post(1L, 10L);
        Comment root = comment(1L, post, 20L, null, "댓글");
        Comment reply = comment(2L, post, 21L, root, "답글");
        Comment nestedReply = comment(3L, post, 22L, reply, "중첩 답글");
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.findAllByPostIdWithParent(1L))
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
        given(commentRepository.findAllByPostIdWithParent(1L))
                .willReturn(List.of(comment));

        CommentListResponse response = commentService.getComments(null, 1L);

        assertThat(response.comments()).hasSize(1);
        assertThat(response.comments().getFirst().isMine()).isFalse();
        assertThat(response.comments().getFirst().isLiked()).isFalse();
        then(commentLikeRepository).should(never()).findLikedCommentIds(any(), any());
    }

    @Test
    void getCommentsUsesNicknamesAndNumbersOnlyAnonymousAuthors() {
        Post post = post(1L, 10L);
        Comment firstAnonymous = comment(1L, post, 20L, null, "첫 번째 익명 댓글");
        Comment named = comment(2L, post, 21L, null, "닉네임 댓글");
        Comment secondAnonymous = comment(3L, post, 22L, null, "두 번째 익명 댓글");
        Comment sameAsFirst = comment(4L, post, 20L, null, "첫 번째 익명 작성자의 추가 댓글");
        User anonymousAuthor1 = user(20L, null);
        User namedAuthor = user(21L, "보듬맘");
        ReflectionTestUtils.setField(
                namedAuthor,
                "profileImageUrl",
                "https://example.com/profile.jpg"
        );
        User anonymousAuthor2 = user(22L, " ");
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.findAllByPostIdWithParent(1L))
                .willReturn(List.of(firstAnonymous, named, secondAnonymous, sameAsFirst));
        given(userRepository.findAllById(any()))
                .willReturn(List.of(anonymousAuthor1, namedAuthor, anonymousAuthor2));

        CommentListResponse response = commentService.getComments(null, 1L);

        assertThat(response.comments())
                .extracting(comment -> comment.authorNickname())
                .containsExactly("익명 1", "보듬맘", "익명 2", "익명 1");
        assertThat(response.comments())
                .extracting(comment -> comment.profileImageUrl())
                .containsExactly(null, "https://example.com/profile.jpg", null, null);
    }

    @Test
    void getCommentsKeepsDeletedParentAsPlaceholderWithActiveReply() {
        Post post = post(1L, 10L);
        Comment deletedRoot = comment(1L, post, 20L, null, "노출하면 안 되는 원문");
        Comment activeReply = comment(2L, post, 21L, deletedRoot, "유지할 답글");
        deletedRoot.delete();
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.findAllByPostIdWithParent(1L))
                .willReturn(List.of(deletedRoot, activeReply));

        CommentListResponse response = commentService.getComments(null, 1L);

        assertThat(response.totalCount()).isOne();
        assertThat(response.comments()).singleElement().satisfies(deleted -> {
            assertThat(deleted.commentId()).isEqualTo(1L);
            assertThat(deleted.parentCommentId()).isNull();
            assertThat(deleted.authorId()).isNull();
            assertThat(deleted.authorNickname()).isNull();
            assertThat(deleted.profileImageUrl()).isNull();
            assertThat(deleted.isMine()).isFalse();
            assertThat(deleted.content()).isEqualTo("삭제된 댓글입니다");
            assertThat(deleted.isAccepted()).isFalse();
            assertThat(deleted.likeCount()).isZero();
            assertThat(deleted.isLiked()).isFalse();
            assertThat(deleted.status()).isEqualTo(CommentStatus.DELETED);
            assertThat(deleted.replies()).singleElement().satisfies(reply -> {
                assertThat(reply.commentId()).isEqualTo(2L);
                assertThat(reply.parentCommentId()).isEqualTo(1L);
                assertThat(reply.content()).isEqualTo("유지할 답글");
            });
        });
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
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void likeCommentGrantsPointToCommentAuthor() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "댓글");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));

        commentService.likeComment(30L, 1L);

        assertThat(comment.getLikeCount()).isOne();
        then(pointService).should().grantActivityPoint(
                20L,
                PointEventType.COMMUNITY_ANSWER_LIKE_RECEIVED,
                1L,
                30L
        );
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
        then(pointService).should().revokeActivityPoint(
                20L,
                PointEventType.COMMUNITY_ANSWER_LIKE_RECEIVED,
                1L,
                30L
        );
    }

    @Test
    void toggleCommentAdoptionAdoptsAndCancelsCommentOnOwnedQuestionPost() {
        Post post = post(1L, 10L, PostBoardType.INFORMATION_QUESTION);
        Comment comment = comment(1L, post, 20L, null, "채택할 답변");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.existsByPost_IdAndAcceptedTrueAndStatusAndDeletedAtIsNull(
                1L,
                CommentStatus.ACTIVE
        )).willReturn(false);

        var adopted = commentService.toggleCommentAdoption(10L, 1L);
        var canceled = commentService.toggleCommentAdoption(10L, 1L);

        assertThat(adopted.isAccepted()).isTrue();
        assertThat(canceled.isAccepted()).isFalse();
        assertThat(comment.isAccepted()).isFalse();
        then(pointService).should().grantActivityPoint(
                20L,
                PointEventType.COMMUNITY_ANSWER_ACCEPTED,
                1L,
                10L
        );
        then(pointService).should().revokeActivityPoint(
                20L,
                PointEventType.COMMUNITY_ANSWER_ACCEPTED,
                1L,
                10L
        );
    }

    @Test
    void toggleCommentAdoptionRejectsNonPostOwner() {
        Post post = post(1L, 10L, PostBoardType.INFORMATION_QUESTION);
        Comment comment = comment(1L, post, 20L, null, "채택할 답변");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.toggleCommentAdoption(30L, 1L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.COMMENT_ADOPTION_FORBIDDEN);
    }

    @Test
    void toggleCommentAdoptionRejectsNonQuestionPost() {
        Post post = post(1L, 10L);
        Comment comment = comment(1L, post, 20L, null, "일반 게시글 댓글");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        assertThatThrownBy(() -> commentService.toggleCommentAdoption(10L, 1L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.POST_NOT_QUESTION);
    }

    @Test
    void toggleCommentAdoptionRejectsWhenAnotherCommentIsAlreadyAccepted() {
        Post post = post(1L, 10L, PostBoardType.INFORMATION_QUESTION);
        Comment comment = comment(1L, post, 20L, null, "새로 채택할 답변");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(commentRepository.existsByPost_IdAndAcceptedTrueAndStatusAndDeletedAtIsNull(
                1L,
                CommentStatus.ACTIVE
        )).willReturn(true);

        assertThatThrownBy(() -> commentService.toggleCommentAdoption(10L, 1L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.COMMENT_ALREADY_ADOPTED);
    }

    @Test
    void toggleCommentAdoptionAnonymizesWithdrawnCommentAuthor() {
        Post post = post(1L, 10L, PostBoardType.INFORMATION_QUESTION);
        Comment comment = comment(1L, post, 20L, null, "탈퇴 회원의 답변");
        given(commentRepository.findActiveByIdForUpdate(1L, CommentStatus.ACTIVE, PostStatus.ACTIVE))
                .willReturn(Optional.of(comment));
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(userRepository.findWithdrawnUserIdsByIdIn(List.of(20L))).willReturn(List.of(20L));

        var response = commentService.toggleCommentAdoption(10L, 1L);

        assertThat(response.authorId()).isNull();
        assertThat(response.authorNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(response.profileImageUrl()).isNull();
    }

    private Post post(Long postId, Long userId) {
        return post(postId, userId, PostBoardType.FREE_COMMUNICATION);
    }

    private Post post(Long postId, Long userId, PostBoardType boardType) {
        Post post = Post.create(
                userId,
                boardType,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용"
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

    private User user(Long userId, String nickname) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "provider-user-id-" + userId,
                "user" + userId + "@example.com",
                nickname
        );
        ReflectionTestUtils.setField(user, "id", userId);
        return user;
    }
}
