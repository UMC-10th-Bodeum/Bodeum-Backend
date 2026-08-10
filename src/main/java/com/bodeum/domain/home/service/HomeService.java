package com.bodeum.domain.home.service;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.home.dto.response.*;
import com.bodeum.domain.home.repository.*;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.InterestCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final String SORT_LATEST = "latest";

    private final HomeNewsRepository homeNewsRepository;
    private final HomePostRepository homePostRepository;
    private final HomeInfoItemRepository homeInfoItemRepository;
    private final HomeUserRepository homeUserRepository;
    private final HomeRegionRepository homeRegionRepository;

    public List<RecommendedNewsResponse> getRecommendedNews(Long userId) {
        List<News> newsList;
        if (userId != null) {
            Region region = getUserRegion(userId);
            if (region != null) {
                newsList = homeNewsRepository.findTopRecommendedByRegion(region.getId(), PageRequest.of(0, 5));
                if (!newsList.isEmpty()) {
                    return toRecommendedNewsResponses(newsList);
                }
            }
        }
        newsList = homeNewsRepository.findTopRecommended(PageRequest.of(0, 5));
        return toRecommendedNewsResponses(newsList);
    }

    private List<RecommendedNewsResponse> toRecommendedNewsResponses(List<News> newsList) {
        Map<Long, Region> regionMap = getRegionMap(newsList.stream()
                .map(News::getRegionId)
                .toList());
        return newsList.stream()
                .map(news -> RecommendedNewsResponse.from(news, regionMap.get(news.getRegionId())))
                .toList();
    }

    public List<PostPreviewResponse> getPostsPreview(String sort, int limit, Long userId) {
        List<Post> posts;
        if (SORT_LATEST.equals(sort)) {
            posts = homePostRepository.findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                    PostStatus.ACTIVE, PageRequest.of(0, limit));
        } else {
            posts = homePostRepository.findTopByPopularity(PostStatus.ACTIVE, PageRequest.of(0, limit));
        }
        return posts.stream()
                .map(post -> PostPreviewResponse.of(post, null))
                .toList();
    }

    public List<RecommendedPostResponse> getRecommendedPosts(int limit, Long userId) {
        List<Post> posts;
        if (userId != null) {
            List<PostBoardType> boardTypes = homeUserRepository.findWithRegionAndInterestsById(userId)
                    .map(user -> user.getInterestCategories().stream()
                            .flatMap(ic -> toBoardTypes(ic).stream())
                            .distinct()
                            .toList())
                    .orElse(List.of());

            if (!boardTypes.isEmpty()) {
                posts = homePostRepository.findTopByBoardTypes(
                        PostStatus.ACTIVE, boardTypes, PageRequest.of(0, limit));
            } else {
                posts = List.of();
            }
            if (posts.isEmpty()) {
                posts = homePostRepository.findTopByPopularity(PostStatus.ACTIVE, PageRequest.of(0, limit));
            }
        } else {
            posts = homePostRepository.findTopByPopularity(PostStatus.ACTIVE, PageRequest.of(0, limit));
        }

        return posts.stream()
                .map(post -> RecommendedPostResponse.of(post, List.of()))
                .toList();
    }

    public List<NewsPreviewResponse> getNewsPreview(NewsType newsType, int limit, Long userId) {
        List<News> newsList;
        if (userId != null) {
            Region region = getUserRegion(userId);
            if (region != null) {
                newsList = homeNewsRepository.findByNewsTypeAndRegion(newsType, region.getId(), PageRequest.of(0, limit));
                if (!newsList.isEmpty()) {
                    return toNewsPreviewResponses(newsList);
                }
            }
        }
        newsList = homeNewsRepository.findByNewsType(newsType, PageRequest.of(0, limit));
        return toNewsPreviewResponses(newsList);
    }

    private List<NewsPreviewResponse> toNewsPreviewResponses(List<News> newsList) {
        Map<Long, Region> regionMap = getRegionMap(newsList.stream()
                .map(News::getRegionId)
                .toList());
        return newsList.stream()
                .map(news -> NewsPreviewResponse.from(news, regionMap.get(news.getRegionId())))
                .toList();
    }

    private Map<Long, Region> getRegionMap(List<Long> regionIds) {
        List<Long> validIds = regionIds.stream().filter(id -> id != null).toList();
        if (validIds.isEmpty()) return Map.of();
        return homeRegionRepository.findAllByIdIn(validIds).stream()
                .collect(Collectors.toMap(Region::getId, r -> r));
    }

    public Optional<BannerResponse> getBanner(Long userId) {
        if (userId != null) {
            List<News> result = homeNewsRepository.findBannerForUser(
                    userId, RecruitmentStatus.CLOSED, PageRequest.of(0, 1));
            if (!result.isEmpty()) {
                return Optional.of(BannerResponse.from(result.get(0)));
            }
        }
        return homeNewsRepository.findBannerForAnonymous(RecruitmentStatus.CLOSED, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(BannerResponse::from);
    }

    public CategoryCountResponse getInfoItemCounts() {
        return CategoryCountResponse.from(homeInfoItemRepository.countByCategory());
    }

    private Region getUserRegion(Long userId) {
        return homeUserRepository.findWithRegionById(userId)
                .map(User::getRegion)
                .orElse(null);
    }

    private List<PostBoardType> toBoardTypes(InterestCategory interestCategory) {
        return switch (interestCategory) {
            case WELFARE_SUBSIDY -> List.of(PostBoardType.INFORMATION_QUESTION);
            case HOSPITAL_HEALTH -> List.of(PostBoardType.TREATMENT_GROWTH_RECORD);
            case PARENTING_COMMUNICATION -> List.of(PostBoardType.FREE_COMMUNICATION);
            case GROWTH_EDUCATION -> List.of(PostBoardType.TREATMENT_GROWTH_RECORD, PostBoardType.INFORMATION_QUESTION);
        };
    }
}
