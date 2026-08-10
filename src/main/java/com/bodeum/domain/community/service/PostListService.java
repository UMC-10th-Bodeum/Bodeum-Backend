package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.response.PostListItemResponse;
import com.bodeum.domain.community.dto.response.PostSearchSuggestionResponse;
import com.bodeum.domain.community.dto.response.PostSearchSuggestionsResponse;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostListSortType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.repository.PostAuthorRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.user.entity.User;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    static final int PAGE_SIZE = 14;
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
            String keyword,
            PostBoardType categoryCode
    ) {
        PostListSortType defaultSortType = userId == null
                ? PostListSortType.VIEW
                : PostListSortType.LATEST;
        PostListSortType sortType = PostListSortType.from(sort, defaultSortType);
        String normalizedKeyword = normalizeKeyword(keyword);
        PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE, sortType.toSort());
        Page<Post> postPage = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                normalizedKeyword,
                categoryCode,
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

    public PostSearchSuggestionsResponse getSearchSuggestions(String keyword, int size) {
        String searchKeyword = trimKeyword(keyword);
        String normalizedKeyword = escapeLikeKeyword(searchKeyword);
        Page<Post> postPage = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                normalizedKeyword,
                null,
                PageRequest.of(0, size, PostListSortType.LATEST.toSort())
        );
        List<PostSearchSuggestionResponse> suggestions = postPage.getContent().stream()
                .map(post -> toSearchSuggestion(post, searchKeyword))
                .distinct()
                .toList();
        return PostSearchSuggestionsResponse.fromSuggestions(suggestions);
    }

    private PostSearchSuggestionResponse toSearchSuggestion(Post post, String keyword) {
        if (containsIgnoreCase(post.getTitle(), keyword)) {
            return PostSearchSuggestionResponse.fromTitle(post.getTitle());
        }

        if (containsIgnoreCase(post.getContent(), keyword)) {
            return PostSearchSuggestionResponse.fromContent(
                    extractContentSnippet(post.getContent(), keyword)
            );
        }

        return PostSearchSuggestionResponse.fromTitle(post.getTitle());
    }

    private String extractContentSnippet(String content, String keyword) {
        int matchIndex = indexOfIgnoreCase(content, keyword);
        if (matchIndex < 0) {
            return content;
        }

        List<SentenceRange> sentences = splitSentences(content);
        int matchedSentenceIndex = findSentenceIndex(sentences, matchIndex);
        if (matchedSentenceIndex < 0) {
            return content;
        }

        int startSentenceIndex = matchedSentenceIndex;
        int endSentenceIndex = Math.min(matchedSentenceIndex + 1, sentences.size() - 1);
        if (startSentenceIndex == endSentenceIndex && startSentenceIndex > 0) {
            startSentenceIndex--;
        }

        SentenceRange startSentence = sentences.get(startSentenceIndex);
        SentenceRange endSentence = sentences.get(endSentenceIndex);
        String snippet = content.substring(startSentence.start(), endSentence.end()).trim();

        if (startSentenceIndex > 0) {
            snippet = "… " + snippet;
        }
        if (endSentenceIndex < sentences.size() - 1) {
            snippet = snippet + " …";
        }
        return snippet;
    }

    private List<SentenceRange> splitSentences(String content) {
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.KOREAN);
        iterator.setText(content);

        List<SentenceRange> sentences = new ArrayList<>();
        int start = iterator.first();
        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            if (!content.substring(start, end).isBlank()) {
                sentences.add(new SentenceRange(start, end));
            }
        }
        return List.copyOf(sentences);
    }

    private int findSentenceIndex(List<SentenceRange> sentences, int matchIndex) {
        for (int index = 0; index < sentences.size(); index++) {
            SentenceRange sentence = sentences.get(index);
            if (sentence.start() <= matchIndex && matchIndex < sentence.end()) {
                return index;
            }
        }
        return -1;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return indexOfIgnoreCase(text, keyword) >= 0;
    }

    private int indexOfIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null || keyword.isBlank()) {
            return -1;
        }
        return text.toLowerCase(Locale.ROOT).indexOf(keyword.toLowerCase(Locale.ROOT));
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
        return escapeLikeKeyword(trimKeyword(keyword));
    }

    private String trimKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    private String escapeLikeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }

        return keyword
                .replace(LIKE_ESCAPE_CHARACTER, LIKE_ESCAPE_CHARACTER.repeat(2))
                .replace("%", LIKE_ESCAPE_CHARACTER + "%")
                .replace("_", LIKE_ESCAPE_CHARACTER + "_");
    }

    private record SentenceRange(int start, int end) {
    }

}
