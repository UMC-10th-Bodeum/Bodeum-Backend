package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.request.CreateCommentRequest;
import com.bodeum.domain.community.dto.request.UpdateCommentRequest;
import com.bodeum.domain.community.dto.response.CommentLikeResponse;
import com.bodeum.domain.community.dto.response.CommentListResponse;
import com.bodeum.domain.community.dto.response.CommentResponse;
import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.CommentLike;
import com.bodeum.domain.community.entity.Post;
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
import com.bodeum.global.common.constant.WithdrawalConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CreateCommentRequest request) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);
        Comment comment = commentRepository.save(Comment.create(post, userId, request.content()));
        if (post.isQuestion()) {
            pointService.grantActivityPoint(
                    userId,
                    PointEventType.COMMUNITY_ANSWER_CREATED,
                    comment.getId(),
                    userId
            );
        }

        return getCommentResponse(comment, userId, false);
    }

    @Transactional
    public CommentResponse createReply(Long userId, Long parentCommentId, CreateCommentRequest request) {
        validateAuthenticatedUser(userId);
        Comment parent = findActiveCommentForUpdate(parentCommentId);
        lockPost(parent.getPost().getId());
        Comment reply = commentRepository.save(Comment.createReply(parent, userId, request.content()));

        return getCommentResponse(reply, userId, false);
    }

    public CommentListResponse getComments(Long userId, Long postId) {
        Post post = findPost(postId);
        List<Comment> comments = commentRepository.findAllActiveByPostIdWithParent(
                postId,
                CommentStatus.ACTIVE
        );
        Set<Long> likedCommentIds = findLikedCommentIds(userId, comments);
        Set<Long> withdrawnAuthorIds = findWithdrawnAuthorIds(comments);
        Map<Long, String> authorDisplayNames = buildAuthorDisplayNames(comments, withdrawnAuthorIds);

        return new CommentListResponse(
                post.getCommentCount(),
                buildCommentTree(
                        comments,
                        userId,
                        likedCommentIds,
                        withdrawnAuthorIds,
                        authorDisplayNames
                )
        );
    }

    @Transactional
    public CommentResponse updateComment(
            Long userId,
            Long commentId,
            UpdateCommentRequest request
    ) {
        Comment comment = getOwnedActiveCommentForUpdate(userId, commentId);
        comment.updateContent(request.content());
        commentRepository.flush();
        boolean liked = commentLikeRepository.existsByComment_IdAndUserId(commentId, userId);

        return getCommentResponse(comment, userId, liked);
    }

    @Transactional
    public CommentResponse toggleCommentAdoption(Long userId, Long commentId) {
        validateAuthenticatedUser(userId);
        Comment comment = findActiveCommentForUpdate(commentId);
        Post post = lockPost(comment.getPost().getId());
        validatePostOwner(userId, post);
        validateQuestionPost(post);

        if (comment.isAccepted()) {
            comment.cancelAcceptance();
            pointService.revokeActivityPoint(
                    comment.getUserId(),
                    PointEventType.COMMUNITY_ANSWER_ACCEPTED,
                    comment.getId(),
                    userId
            );
        } else {
            validateNoAcceptedComment(post.getId());
            comment.accept();
            pointService.grantActivityPoint(
                    comment.getUserId(),
                    PointEventType.COMMUNITY_ANSWER_ACCEPTED,
                    comment.getId(),
                    userId
            );
        }

        commentRepository.flush();
        boolean liked = commentLikeRepository.existsByComment_IdAndUserId(commentId, userId);
        return getCommentResponse(comment, userId, liked);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = getOwnedActiveCommentForUpdate(userId, commentId);
        lockPost(comment.getPost().getId());
        comment.delete();
    }

    @Transactional
    public CommentLikeResponse likeComment(Long userId, Long commentId) {
        validateAuthenticatedUser(userId);
        Comment comment = findActiveCommentForUpdate(commentId);

        if (!commentLikeRepository.existsByComment_IdAndUserId(commentId, userId)) {
            commentLikeRepository.save(CommentLike.create(comment, userId));
            comment.increaseLikeCount();
            grantCommentLikePoint(comment, userId);
        }

        return new CommentLikeResponse(true, comment.getLikeCount());
    }

    @Transactional
    public CommentLikeResponse unlikeComment(Long userId, Long commentId) {
        validateAuthenticatedUser(userId);
        Comment comment = findActiveCommentForUpdate(commentId);

        Optional<CommentLike> commentLike = commentLikeRepository.findByComment_IdAndUserId(commentId, userId);
        if (commentLike.isPresent()) {
            commentLikeRepository.delete(commentLike.get());
            comment.decreaseLikeCount();
            revokeCommentLikePoint(comment, userId);
        }

        return new CommentLikeResponse(false, comment.getLikeCount());
    }

    // 회원 탈퇴 시: 해당 회원의 댓글·답글 공감을 삭제하고 각 댓글의 likeCount를 감소시킨다.
    // 댓글·답글 본문은 보존 대상이므로 삭제하지 않는다. 카운트 감소를 삭제보다 먼저 수행한다.
    @Transactional
    public void deleteUserCommentLikes(Long userId) {
        commentLikeRepository.findPointRewardReferencesByUserId(userId)
                .forEach(reference -> pointService.revokeActivityPoint(
                        reference.getRecipientUserId(),
                        PointEventType.COMMUNITY_ANSWER_LIKE_RECEIVED,
                        reference.getReferenceId(),
                        userId
                ));
        commentLikeRepository.decreaseLikeCountForUserLikes(userId);
        commentLikeRepository.deleteByUserId(userId);
    }

    private void grantCommentLikePoint(Comment comment, Long actorUserId) {
        if (Objects.equals(comment.getUserId(), actorUserId)) {
            return;
        }

        pointService.grantActivityPoint(
                comment.getUserId(),
                PointEventType.COMMUNITY_ANSWER_LIKE_RECEIVED,
                comment.getId(),
                actorUserId
        );
    }

    private void revokeCommentLikePoint(Comment comment, Long actorUserId) {
        if (Objects.equals(comment.getUserId(), actorUserId)) {
            return;
        }

        pointService.revokeActivityPoint(
                comment.getUserId(),
                PointEventType.COMMUNITY_ANSWER_LIKE_RECEIVED,
                comment.getId(),
                actorUserId
        );
    }

    private Comment getOwnedActiveCommentForUpdate(Long userId, Long commentId) {
        validateAuthenticatedUser(userId);
        Comment comment = findActiveCommentForUpdate(commentId);
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new CommunityException(CommunityErrorCode.COMMENT_FORBIDDEN);
        }
        return comment;
    }

    private Comment findActiveCommentForUpdate(Long commentId) {
        return commentRepository.findActiveByIdForUpdate(
                        commentId,
                        CommentStatus.ACTIVE,
                        PostStatus.ACTIVE
                )
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.COMMENT_NOT_FOUND));
    }

    private Post findPost(Long postId) {
        return postRepository.findByIdAndStatusAndDeletedAtIsNull(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    private Post findPostForUpdate(Long postId) {
        return postRepository.findByIdAndStatusForUpdate(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    private Post lockPost(Long postId) {
        return findPostForUpdate(postId);
    }

    private void validatePostOwner(Long userId, Post post) {
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new CommunityException(CommunityErrorCode.COMMENT_ADOPTION_FORBIDDEN);
        }
    }

    private void validateQuestionPost(Post post) {
        if (!post.isQuestion()) {
            throw new CommunityException(CommunityErrorCode.POST_NOT_QUESTION);
        }
    }

    private void validateNoAcceptedComment(Long postId) {
        if (commentRepository.existsByPost_IdAndAcceptedTrueAndStatusAndDeletedAtIsNull(
                postId,
                CommentStatus.ACTIVE
        )) {
            throw new CommunityException(CommunityErrorCode.COMMENT_ALREADY_ADOPTED);
        }
    }

    private CommentResponse getCommentResponse(Comment comment, Long viewerId, boolean liked) {
        boolean authorWithdrawn = !userRepository
                .findWithdrawnUserIdsByIdIn(List.of(comment.getUserId()))
                .isEmpty();
        Long parentCommentId = comment.getParent() == null ? null : comment.getParent().getId();
        String authorDisplayName = resolveAuthorDisplayName(comment, authorWithdrawn);

        return CommentResponse.of(
                comment,
                parentCommentId,
                viewerId,
                liked,
                authorWithdrawn,
                authorDisplayName,
                List.of()
        );
    }

    private String resolveAuthorDisplayName(Comment comment, boolean authorWithdrawn) {
        if (authorWithdrawn) {
            return WithdrawalConstants.WITHDRAWN_DISPLAY_NAME;
        }

        List<Comment> postComments = new ArrayList<>(commentRepository.findAllActiveByPostIdWithParent(
                comment.getPost().getId(),
                CommentStatus.ACTIVE
        ));
        boolean currentCommentIncluded = postComments.stream()
                .anyMatch(postComment -> Objects.equals(postComment.getId(), comment.getId()));
        if (!currentCommentIncluded) {
            postComments.add(comment);
        }

        Set<Long> withdrawnAuthorIds = findWithdrawnAuthorIds(postComments);
        return buildAuthorDisplayNames(postComments, withdrawnAuthorIds).get(comment.getUserId());
    }

    private Set<Long> findLikedCommentIds(Long userId, List<Comment> comments) {
        if (userId == null) {
            return Set.of();
        }

        List<Long> activeCommentIds = comments.stream()
                .filter(comment -> comment.getStatus() == CommentStatus.ACTIVE && !comment.isDeleted())
                .map(Comment::getId)
                .toList();

        if (activeCommentIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(commentLikeRepository.findLikedCommentIds(userId, activeCommentIds));
    }

    // 보존된 댓글·답글의 작성자 익명화용. 페이지 내 distinct 작성자 id를 배치 조회해 탈퇴 회원 집합만 판정한다(N+1 방지).
    private Set<Long> findWithdrawnAuthorIds(List<Comment> comments) {
        Set<Long> authorIds = comments.stream()
                .map(Comment::getUserId)
                .collect(Collectors.toSet());
        if (authorIds.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(userRepository.findWithdrawnUserIdsByIdIn(authorIds));
    }

    private Map<Long, String> buildAuthorDisplayNames(
            List<Comment> comments,
            Set<Long> withdrawnAuthorIds
    ) {
        List<Comment> orderedComments = comments.stream()
                .sorted(Comparator.comparing(Comment::getId))
                .toList();
        Set<Long> activeAuthorIds = orderedComments.stream()
                .map(Comment::getUserId)
                .filter(authorId -> !withdrawnAuthorIds.contains(authorId))
                .collect(Collectors.toSet());
        Map<Long, User> authorsById = userRepository.findAllById(activeAuthorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, String> displayNamesByAuthorId = new LinkedHashMap<>();
        int anonymousNumber = 1;
        for (Comment comment : orderedComments) {
            Long authorId = comment.getUserId();
            if (displayNamesByAuthorId.containsKey(authorId)) {
                continue;
            }
            if (withdrawnAuthorIds.contains(authorId)) {
                displayNamesByAuthorId.put(authorId, WithdrawalConstants.WITHDRAWN_DISPLAY_NAME);
                continue;
            }

            User author = authorsById.get(authorId);
            String nickname = author == null ? null : author.getNickname();
            if (nickname != null && !nickname.isBlank()) {
                displayNamesByAuthorId.put(authorId, nickname);
                continue;
            }

            displayNamesByAuthorId.put(authorId, "익명 " + anonymousNumber);
            anonymousNumber++;
        }
        return Map.copyOf(displayNamesByAuthorId);
    }

    private List<CommentResponse> buildCommentTree(
            List<Comment> comments,
            Long viewerId,
            Set<Long> likedCommentIds,
            Set<Long> withdrawnAuthorIds,
            Map<Long, String> authorDisplayNames
    ) {
        List<Comment> orderedComments = comments.stream()
                .sorted(Comparator.comparing(Comment::getId))
                .toList();
        Map<Long, Comment> commentsById = orderedComments.stream()
                .collect(Collectors.toMap(Comment::getId, Function.identity()));
        Map<Long, List<CommentResponse>> repliesByParentId = new HashMap<>();
        List<CommentResponse> roots = new ArrayList<>();

        for (int index = orderedComments.size() - 1; index >= 0; index--) {
            Comment comment = orderedComments.get(index);
            List<CommentResponse> replies = reverseCopy(repliesByParentId.get(comment.getId()));
            Long parentId = comment.getParent() == null ? null : comment.getParent().getId();
            Long visibleParentId = parentId != null && commentsById.containsKey(parentId) ? parentId : null;
            CommentResponse response = CommentResponse.of(
                    comment,
                    visibleParentId,
                    viewerId,
                    likedCommentIds.contains(comment.getId()),
                    withdrawnAuthorIds.contains(comment.getUserId()),
                    authorDisplayNames.get(comment.getUserId()),
                    replies
            );

            if (visibleParentId == null) {
                roots.add(response);
            } else {
                repliesByParentId.computeIfAbsent(visibleParentId, ignored -> new ArrayList<>()).add(response);
            }
        }

        Collections.reverse(roots);
        return List.copyOf(roots);
    }

    private List<CommentResponse> reverseCopy(List<CommentResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return List.of();
        }

        List<CommentResponse> orderedResponses = new ArrayList<>(responses);
        Collections.reverse(orderedResponses);
        return List.copyOf(orderedResponses);
    }

    private void validateAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new CommunityException(CommunityErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
