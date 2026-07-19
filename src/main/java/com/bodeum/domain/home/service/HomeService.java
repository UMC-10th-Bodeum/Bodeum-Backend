package com.bodeum.domain.home.service;

import com.bodeum.domain.community.entity.Post;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

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
        List<Post> posts = "latest".equals(sort)
                ? homePostRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit))
                : homePostRepository.findTopByPopularity(PageRequest.of(0, limit));

        return posts.stream()
                .map(post -> PostPreviewResponse.of(
                        post,
                        homePostLikeRepository.countByPostId(post.getId()),
                        homeCommentRepository.countByPostId(post.getId())
                ))
                .toList();
    }

    public List<RecommendedPostResponse> getRecommendedPosts(int limit) {
        return homePostRepository.findTopByPopularity(PageRequest.of(0, limit))
                .stream()
                .map(post -> {
                    List<DisabilityType> tags = postDisabilityTagRepository
                            .findAllByPost_IdOrderByIdAsc(post.getId())
                            .stream()
                            .map(tag -> tag.getDisabilityType())
                            .toList();
                    return RecommendedPostResponse.of(
                            post,
                            tags,
                            homePostLikeRepository.countByPostId(post.getId()),
                            homeCommentRepository.countByPostId(post.getId())
                    );
                })
                .toList();
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
