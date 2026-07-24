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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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

    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CreateCommentRequest request) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);
        Comment comment = commentRepository.save(Comment.create(post, userId, request.content()));

        return CommentResponse.of(comment, userId, false, List.of());
    }

    @Transactional
    public CommentResponse createReply(Long userId, Long parentCommentId, CreateCommentRequest request) {
        validateAuthenticatedUser(userId);
        Comment parent = findActiveCommentForUpdate(parentCommentId);
        lockPost(parent.getPost().getId());
        Comment reply = commentRepository.save(Comment.createReply(parent, userId, request.content()));

        return CommentResponse.of(reply, userId, false, List.of());
    }

    public CommentListResponse getComments(Long userId, Long postId) {
        Post post = findPost(postId);
        List<Comment> comments = commentRepository.findAllActiveByPostIdWithParent(
                postId,
                CommentStatus.ACTIVE
        );
        Set<Long> likedCommentIds = findLikedCommentIds(userId, comments);

        return new CommentListResponse(
                post.getCommentCount(),
                buildCommentTree(comments, userId, likedCommentIds)
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

        return CommentResponse.of(comment, userId, liked, List.of());
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
        }

        return new CommentLikeResponse(false, comment.getLikeCount());
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

    private void lockPost(Long postId) {
        findPostForUpdate(postId);
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

    private List<CommentResponse> buildCommentTree(
            List<Comment> comments,
            Long viewerId,
            Set<Long> likedCommentIds
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
