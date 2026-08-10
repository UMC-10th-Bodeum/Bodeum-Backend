package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostLikeResponse;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.dto.response.PostScrapResponse;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostImage;
import com.bodeum.domain.community.entity.PostLike;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.CommentRepository;
import com.bodeum.domain.community.repository.PostAuthorRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.GuardianLevel;
import com.bodeum.domain.user.repository.UserRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final PostAuthorRepository postAuthorRepository;
    private final UserRepository userRepository;
    private final PointService pointService;

    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        validateAuthenticatedUser(userId);

        Post post = postRepository.save(Post.create(
                userId,
                request.boardType(),
                request.anonymityType(),
                request.title(),
                request.content()
        ));

        saveImages(post, safeList(request.imageUrls()));
        pointService.grantActivityPoint(
                userId,
                PointEventType.COMMUNITY_POST_CREATED,
                post.getId(),
                userId
        );

        return getPostResponse(post, userId);
    }

    @Transactional
    public PostResponse updatePost(Long userId, Long postId, UpdatePostRequest request) {
        Post post = getOwnedPost(userId, postId);
        PostBoardType targetBoardType = request.boardType() == null
                ? post.getBoardType()
                : request.boardType();
        validateBoardTypeChange(post, targetBoardType);

        post.update(
                targetBoardType,
                request.anonymityType() == null ? post.getAnonymityType() : request.anonymityType(),
                request.title() == null ? post.getTitle() : request.title(),
                request.content() == null ? post.getContent() : request.content()
        );

        if (request.imageUrls() != null) {
            replaceImages(post, request.imageUrls());
        }

        return getPostResponse(post, userId);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = getOwnedPost(userId, postId);

        post.delete();
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(Long userId, Long postId) {
        return getPostResponse(findPost(postId), userId);
    }

    @Transactional
    public PostLikeResponse likePost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);

        if (!postLikeRepository.existsByPost_IdAndUserId(postId, userId)) {
            postLikeRepository.save(PostLike.create(post, userId));
            post.increaseLikeCount();
            grantPostLikePoint(post, userId);
        }

        return new PostLikeResponse(true, post.getLikeCount());
    }

    @Transactional
    public PostLikeResponse unlikePost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);

        Optional<PostLike> postLike = postLikeRepository.findByPost_IdAndUserId(postId, userId);
        if (postLike.isPresent()) {
            postLikeRepository.delete(postLike.get());
            post.decreaseLikeCount();
            revokePostLikePoint(post, userId);
        }

        return new PostLikeResponse(false, post.getLikeCount());
    }

    @Transactional
    public PostScrapResponse scrapPost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);

        if (!postScrapRepository.existsByPost_IdAndUserId(postId, userId)) {
            postScrapRepository.save(PostScrap.create(post, userId));
            post.increaseScrapCount();
        }

        return new PostScrapResponse(true, post.getScrapCount());
    }

    @Transactional
    public PostScrapResponse unscrapPost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);

        Optional<PostScrap> postScrap = postScrapRepository.findByPost_IdAndUserId(postId, userId);
        if (postScrap.isPresent()) {
            postScrapRepository.delete(postScrap.get());
            post.decreaseScrapCount();
        }

        return new PostScrapResponse(false, post.getScrapCount());
    }

    // 회원 탈퇴 시: 해당 회원의 게시글 스크랩·좋아요를 삭제하고 각 게시글의 scrapCount·likeCount를 감소시킨다.
    // 게시글 본문은 보존 대상이므로 삭제하지 않는다. 카운트 감소를 삭제보다 먼저 수행한다.
    @Transactional
    public void deleteUserScrapsAndLikes(Long userId) {
        postLikeRepository.findPointRewardReferencesByUserId(userId)
                .forEach(reference -> pointService.revokeActivityPoint(
                        reference.getRecipientUserId(),
                        PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                        reference.getReferenceId(),
                        userId
                ));
        postScrapRepository.decreaseScrapCountForUserScraps(userId);
        postScrapRepository.deleteByUserId(userId);
        postLikeRepository.decreaseLikeCountForUserLikes(userId);
        postLikeRepository.deleteByUserId(userId);
    }

    private void grantPostLikePoint(Post post, Long actorUserId) {
        if (Objects.equals(post.getUserId(), actorUserId)) {
            return;
        }

        pointService.grantActivityPoint(
                post.getUserId(),
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                post.getId(),
                actorUserId
        );
    }

    private void revokePostLikePoint(Post post, Long actorUserId) {
        if (Objects.equals(post.getUserId(), actorUserId)) {
            return;
        }

        pointService.revokeActivityPoint(
                post.getUserId(),
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                post.getId(),
                actorUserId
        );
    }

    private Post getOwnedPost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new CommunityException(CommunityErrorCode.POST_FORBIDDEN);
        }
        return post;
    }

    private void validateBoardTypeChange(Post post, PostBoardType targetBoardType) {
        if (!post.isQuestion() || targetBoardType == PostBoardType.INFORMATION_QUESTION) {
            return;
        }

        if (commentRepository.existsByPost_IdAndAcceptedTrueAndStatusAndDeletedAtIsNull(
                post.getId(),
                CommentStatus.ACTIVE
        )) {
            throw new CommunityException(
                    CommunityErrorCode.POST_BOARD_CHANGE_BLOCKED_BY_ADOPTED_COMMENT
            );
        }
    }

    private Post findPost(Long postId) {
        return postRepository.findByIdAndStatusAndDeletedAtIsNull(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    private Post findPostForUpdate(Long postId) {
        return postRepository.findByIdAndStatusForUpdate(postId, PostStatus.ACTIVE)
                .orElseThrow(() -> new CommunityException(CommunityErrorCode.POST_NOT_FOUND));
    }

    private PostResponse getPostResponse(Post post, Long viewerId) {
        boolean liked = viewerId != null
                && postLikeRepository.existsByPost_IdAndUserId(post.getId(), viewerId);
        boolean scrapped = viewerId != null
                && postScrapRepository.existsByPost_IdAndUserId(post.getId(), viewerId);
        List<String> imageUrls = postImageRepository
                .findAllByPost_IdOrderBySortOrderAsc(post.getId())
                .stream()
                .map(PostImage::getImageUrl)
                .toList();
        boolean authorWithdrawn = !userRepository
                .findWithdrawnUserIdsByIdIn(List.of(post.getUserId()))
                .isEmpty();

        User author = null;
        Integer authorLevel = null;
        Integer childAge = null;
        if (!authorWithdrawn && post.getAnonymityType() != PostAnonymityType.FULLY_ANONYMOUS) {
            author = postAuthorRepository.findById(post.getUserId()).orElse(null);
        }
        if (author != null) {
            authorLevel = GuardianLevel.from(pointService.getTotalPoint(author.getId())).getLevelNumber();
            childAge = author.getChildAge();
        }

        return PostResponse.of(post, viewerId, liked, scrapped, authorWithdrawn, authorLevel, childAge,
                imageUrls);
    }

    private void replaceImages(Post post, List<String> imageUrls) {
        postImageRepository.deleteAllByPost_Id(post.getId());
        postImageRepository.flush();
        saveImages(post, imageUrls);
    }

    private void saveImages(Post post, List<String> imageUrls) {
        postImageRepository.saveAll(java.util.stream.IntStream.range(0, imageUrls.size())
                .mapToObj(index -> PostImage.create(post, imageUrls.get(index), index))
                .toList());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void validateAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new CommunityException(CommunityErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
