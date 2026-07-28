package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostLikeResponse;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.dto.response.PostScrapResponse;
import com.bodeum.domain.community.entity.Hashtag;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostDisabilityTag;
import com.bodeum.domain.community.entity.PostHashtag;
import com.bodeum.domain.community.entity.PostImage;
import com.bodeum.domain.community.entity.PostLike;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.HashtagRepository;
import com.bodeum.domain.community.repository.PostDisabilityTagRepository;
import com.bodeum.domain.community.repository.PostHashtagRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
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
    private final HashtagRepository hashtagRepository;
    private final PostHashtagRepository postHashtagRepository;
    private final PostImageRepository postImageRepository;
    private final PostDisabilityTagRepository postDisabilityTagRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;

    @Transactional
    public PostResponse createPost(Long userId, CreatePostRequest request) {
        validateAuthenticatedUser(userId);

        Post post = postRepository.save(Post.create(
                userId,
                request.boardType(),
                request.anonymityType(),
                request.title(),
                request.content(),
                request.isQuestion()
        ));

        saveDisabilityTags(post, safeList(request.disabilityTypes()));
        saveHashtags(post, normalizeHashtags(request.hashtags()));
        saveImages(post, safeList(request.imageUrls()));

        return getPostResponse(post, userId);
    }

    @Transactional
    public PostResponse updatePost(Long userId, Long postId, UpdatePostRequest request) {
        Post post = getOwnedPost(userId, postId);
        post.update(
                request.boardType() == null ? post.getBoardType() : request.boardType(),
                request.anonymityType() == null ? post.getAnonymityType() : request.anonymityType(),
                request.title() == null ? post.getTitle() : request.title(),
                request.content() == null ? post.getContent() : request.content(),
                request.isQuestion() == null ? post.isQuestion() : request.isQuestion()
        );

        if (request.disabilityTypes() != null) {
            replaceDisabilityTags(post, request.disabilityTypes());
        }
        if (request.hashtags() != null) {
            replaceHashtags(post, normalizeHashtags(request.hashtags()));
        }
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
        validateAuthenticatedUser(userId);
        return getPostResponse(findPost(postId), userId);
    }

    @Transactional
    public PostLikeResponse likePost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);

        if (!postLikeRepository.existsByPost_IdAndUserId(postId, userId)) {
            postLikeRepository.save(PostLike.create(post, userId));
            post.increaseLikeCount();
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

    private Post getOwnedPost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        Post post = findPostForUpdate(postId);
        if (!Objects.equals(post.getUserId(), userId)) {
            throw new CommunityException(CommunityErrorCode.POST_FORBIDDEN);
        }
        return post;
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
        boolean liked = postLikeRepository.existsByPost_IdAndUserId(post.getId(), viewerId);
        boolean scrapped = postScrapRepository.existsByPost_IdAndUserId(post.getId(), viewerId);
        List<DisabilityType> disabilityTypes = postDisabilityTagRepository
                .findAllByPost_IdOrderByIdAsc(post.getId())
                .stream()
                .map(PostDisabilityTag::getDisabilityType)
                .toList();
        List<String> hashtags = postHashtagRepository
                .findAllByPost_IdOrderByIdAsc(post.getId())
                .stream()
                .map(PostHashtag::getHashtag)
                .map(Hashtag::getName)
                .toList();
        List<String> imageUrls = postImageRepository
                .findAllByPost_IdOrderBySortOrderAsc(post.getId())
                .stream()
                .map(PostImage::getImageUrl)
                .toList();

        return PostResponse.of(post, viewerId, liked, scrapped, disabilityTypes, hashtags, imageUrls);
    }

    private void replaceDisabilityTags(Post post, List<DisabilityType> disabilityTypes) {
        postDisabilityTagRepository.deleteAllByPost_Id(post.getId());
        postDisabilityTagRepository.flush();
        saveDisabilityTags(post, disabilityTypes);
    }

    private void saveDisabilityTags(Post post, List<DisabilityType> disabilityTypes) {
        postDisabilityTagRepository.saveAll(disabilityTypes.stream()
                .distinct()
                .map(disabilityType -> PostDisabilityTag.create(post, disabilityType))
                .toList());
    }

    private void replaceHashtags(Post post, List<String> hashtagNames) {
        postHashtagRepository.deleteAllByPost_Id(post.getId());
        postHashtagRepository.flush();
        saveHashtags(post, hashtagNames);
    }

    private void saveHashtags(Post post, List<String> hashtagNames) {
        postHashtagRepository.saveAll(hashtagNames.stream()
                .map(this::getOrCreateHashtag)
                .map(hashtag -> PostHashtag.create(post, hashtag))
                .toList());
    }

    private Hashtag getOrCreateHashtag(String name) {
        return hashtagRepository.findByName(name)
                .orElseGet(() -> hashtagRepository.save(Hashtag.create(name)));
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

    private List<String> normalizeHashtags(List<String> hashtags) {
        return safeList(hashtags).stream()
                .map(String::trim)
                .distinct()
                .toList();
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
