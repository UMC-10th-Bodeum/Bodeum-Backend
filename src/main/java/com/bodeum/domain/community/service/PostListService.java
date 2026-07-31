package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.response.PostListItemResponse;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostListSortType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.repository.PostAuthorRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.user.entity.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostListService {

    static final int PAGE_SIZE = 10;
    private static final String LIKE_ESCAPE_CHARACTER = "!";

    private final PostRepository postRepository;
    private final PostAuthorRepository postAuthorRepository;
    private final PostImageRepository postImageRepository;
    private final PostLikeRepository postLikeRepository;
    private final PointService pointService;

    public Page<PostListItemResponse> getPosts(
            Long userId,
            int page,
            String sort,
            String keyword
    ) {
        PostListSortType sortType = PostListSortType.from(sort);
        String normalizedKeyword = normalizeKeyword(keyword);
        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE, sortType.toSort());
        Page<Post> postPage = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                normalizedKeyword,
                pageRequest
        );

        List<Post> posts = postPage.getContent();
        if (posts.isEmpty()) {
            return new PageImpl<>(List.of(), postPage.getPageable(), postPage.getTotalElements());
        }

        List<Long> postIds = posts.stream()
                .map(Post::getId)
                .toList();
        Map<Long, User> authorsById = getAuthorsById(posts);
        Map<Long, Integer> totalPointsByAuthorId = authorsById.isEmpty()
                ? Map.of()
                : pointService.getTotalPoints(authorsById.keySet());
        Map<Long, String> thumbnailsByPostId = getThumbnailsByPostId(postIds);
        Set<Long> likedPostIds = getLikedPostIds(userId, postIds);

        List<PostListItemResponse> responses = posts.stream()
                .map(post -> PostListItemResponse.of(
                        post,
                        authorsById.get(post.getUserId()),
                        totalPointsByAuthorId.getOrDefault(post.getUserId(), 0),
                        userId,
                        thumbnailsByPostId.get(post.getId()),
                        likedPostIds.contains(post.getId())
                ))
                .toList();

        return new PageImpl<>(responses, postPage.getPageable(), postPage.getTotalElements());
    }

    private Map<Long, User> getAuthorsById(List<Post> posts) {
        List<Long> authorIds = posts.stream()
                .filter(post -> post.getAnonymityType() == PostAnonymityType.PROFILE_TAG_VISIBLE)
                .map(Post::getUserId)
                .distinct()
                .toList();
        if (authorIds.isEmpty()) {
            return Map.of();
        }

        return postAuthorRepository.findAllByIdIn(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private Map<Long, String> getThumbnailsByPostId(List<Long> postIds) {
        Map<Long, String> thumbnailsByPostId = new LinkedHashMap<>();
        postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(postIds)
                .forEach(image -> thumbnailsByPostId.putIfAbsent(
                        image.getPost().getId(),
                        image.getImageUrl()
                ));
        return thumbnailsByPostId;
    }

    private Set<Long> getLikedPostIds(Long userId, List<Long> postIds) {
        if (userId == null) {
            return Set.of();
        }
        return Set.copyOf(postLikeRepository.findLikedPostIds(postIds, userId));
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim()
                .replace(LIKE_ESCAPE_CHARACTER, LIKE_ESCAPE_CHARACTER.repeat(2))
                .replace("%", LIKE_ESCAPE_CHARACTER + "%")
                .replace("_", LIKE_ESCAPE_CHARACTER + "_");
    }

}
