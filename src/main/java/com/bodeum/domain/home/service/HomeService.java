package com.bodeum.domain.home.service;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostDisabilityTag;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.repository.PostDisabilityTagRepository;
import com.bodeum.domain.home.dto.response.*;
import com.bodeum.domain.home.repository.*;
import com.bodeum.domain.news.entity.NewsType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final String SORT_LATEST = "latest";

    private final HomeNewsRepository homeNewsRepository;
    private final HomePostRepository homePostRepository;
    private final HomePostLikeRepository homePostLikeRepository;
    private final HomeCommentRepository homeCommentRepository;
    private final HomeInfoItemRepository homeInfoItemRepository;
    private final PostDisabilityTagRepository postDisabilityTagRepository;

    public List<RecommendedNewsResponse> getRecommendedNews() {
        return homeNewsRepository.findTopRecommended(PageRequest.of(0, 5))
                .stream()
                .map(RecommendedNewsResponse::from)
                .toList();
    }

    public List<PostPreviewResponse> getPostsPreview(String sort, int limit) {
        List<Post> posts = SORT_LATEST.equals(sort)
                ? homePostRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                : homePostRepository.findTopByPopularity(PageRequest.of(0, limit));

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, Long> likeCountMap = toCountMap(homePostLikeRepository.countGroupByPostIdIn(postIds));
        Map<Long, Long> commentCountMap = toCountMap(homeCommentRepository.countGroupByPostIdIn(postIds));

        return posts.stream()
                .map(post -> PostPreviewResponse.of(
                        post,
                        likeCountMap.getOrDefault(post.getId(), 0L),
                        commentCountMap.getOrDefault(post.getId(), 0L)
                ))
                .toList();
    }

    public List<RecommendedPostResponse> getRecommendedPosts(int limit) {
        List<Post> posts = homePostRepository.findTopByPopularity(PageRequest.of(0, limit));

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, List<DisabilityType>> tagsMap = postDisabilityTagRepository.findAllByPost_IdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getPost().getId(),
                        Collectors.mapping(PostDisabilityTag::getDisabilityType, Collectors.toList())
                ));
        Map<Long, Long> likeCountMap = toCountMap(homePostLikeRepository.countGroupByPostIdIn(postIds));
        Map<Long, Long> commentCountMap = toCountMap(homeCommentRepository.countGroupByPostIdIn(postIds));

        return posts.stream()
                .map(post -> RecommendedPostResponse.of(
                        post,
                        tagsMap.getOrDefault(post.getId(), List.of()),
                        likeCountMap.getOrDefault(post.getId(), 0L),
                        commentCountMap.getOrDefault(post.getId(), 0L)
                ))
                .toList();
    }

    private Map<Long, Long> toCountMap(List<Object[]> rows) {
        return rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    public List<NewsPreviewResponse> getNewsPreview(NewsType newsType, int limit) {
        return homeNewsRepository.findByNewsType(newsType, PageRequest.of(0, limit))
                .stream()
                .map(NewsPreviewResponse::from)
                .toList();
    }

    public CategoryCountResponse getInfoItemCounts() {
        return CategoryCountResponse.from(homeInfoItemRepository.countByCategory());
    }
}
