package com.bodeum.domain.home.service;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostDisabilityTag;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.repository.PostDisabilityTagRepository;
import com.bodeum.domain.home.dto.response.*;
import com.bodeum.domain.home.repository.*;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.InterestCategory;
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
    private final HomeInfoItemRepository homeInfoItemRepository;
    private final HomeUserRepository homeUserRepository;
    private final PostDisabilityTagRepository postDisabilityTagRepository;

    public List<RecommendedNewsResponse> getRecommendedNews(Long userId) {
        if (userId != null) {
            Region region = getUserRegion(userId);
            if (region != null) {
                return homeNewsRepository.findTopRecommendedByRegion(region.getId(), PageRequest.of(0, 5))
                        .stream()
                        .map(RecommendedNewsResponse::from)
                        .toList();
            }
        }
        return homeNewsRepository.findTopRecommended(PageRequest.of(0, 5))
                .stream()
                .map(RecommendedNewsResponse::from)
                .toList();
    }

    public List<PostPreviewResponse> getPostsPreview(String sort, int limit, Long userId) {
        Region region = userId != null ? getUserRegion(userId) : null;
        List<Post> posts;
        if (SORT_LATEST.equals(sort)) {
            posts = region != null
                    ? homePostRepository.findAllByStatusAndRegionOrderByCreatedAtDesc(
                            PostStatus.ACTIVE, region.getId(), PageRequest.of(0, limit))
                    : homePostRepository.findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                            PostStatus.ACTIVE, PageRequest.of(0, limit));
        } else {
            posts = region != null
                    ? homePostRepository.findTopByPopularityAndRegion(
                            PostStatus.ACTIVE, region.getId(), PageRequest.of(0, limit))
                    : homePostRepository.findTopByPopularity(PostStatus.ACTIVE, PageRequest.of(0, limit));
        }
        String regionName = region != null ? region.getFullName() : null;
        return posts.stream()
                .map(post -> PostPreviewResponse.of(post, regionName))
                .toList();
    }

    public List<RecommendedPostResponse> getRecommendedPosts(int limit, Long userId) {
        List<Post> posts;
        if (userId != null) {
            // 장애 유형과 관심사는 각각 다른 컬렉션 fetch가 필요해 쿼리를 분리
            List<DisabilityType> disabilityTypes = getUserDisabilityTypes(userId);
            List<PostBoardType> boardTypes = homeUserRepository.findWithRegionAndInterestsById(userId)
                    .map(user -> user.getInterestCategories().stream()
                            .flatMap(ic -> toBoardTypes(ic).stream())
                            .distinct()
                            .toList())
                    .orElse(List.of());

            if (!disabilityTypes.isEmpty() && !boardTypes.isEmpty()) {
                posts = homePostRepository.findTopByPersonalization(
                        PostStatus.ACTIVE, disabilityTypes, boardTypes, PageRequest.of(0, limit));
            } else if (!disabilityTypes.isEmpty()) {
                posts = homePostRepository.findTopByDisabilityTypes(
                        PostStatus.ACTIVE, disabilityTypes, PageRequest.of(0, limit));
            } else if (!boardTypes.isEmpty()) {
                posts = homePostRepository.findTopByBoardTypes(
                        PostStatus.ACTIVE, boardTypes, PageRequest.of(0, limit));
            } else {
                posts = homePostRepository.findTopByPopularity(PostStatus.ACTIVE, PageRequest.of(0, limit));
            }
        } else {
            posts = homePostRepository.findTopByPopularity(PostStatus.ACTIVE, PageRequest.of(0, limit));
        }

        List<Long> postIds = posts.stream().map(Post::getId).toList();
        Map<Long, List<DisabilityType>> tagsMap = postDisabilityTagRepository.findAllByPost_IdIn(postIds)
                .stream()
                .collect(Collectors.groupingBy(
                        tag -> tag.getPost().getId(),
                        Collectors.mapping(PostDisabilityTag::getDisabilityType, Collectors.toList())
                ));

        return posts.stream()
                .map(post -> RecommendedPostResponse.of(post, tagsMap.getOrDefault(post.getId(), List.of())))
                .toList();
    }

    public List<NewsPreviewResponse> getNewsPreview(NewsType newsType, int limit, Long userId) {
        if (userId != null) {
            Region region = getUserRegion(userId);
            if (region != null) {
                return homeNewsRepository.findByNewsTypeAndRegion(newsType, region.getId(), PageRequest.of(0, limit))
                        .stream()
                        .map(NewsPreviewResponse::from)
                        .toList();
            }
        }
        return homeNewsRepository.findByNewsType(newsType, PageRequest.of(0, limit))
                .stream()
                .map(NewsPreviewResponse::from)
                .toList();
    }

    public CategoryCountResponse getInfoItemCounts() {
        return CategoryCountResponse.from(homeInfoItemRepository.countByCategory());
    }

    private Region getUserRegion(Long userId) {
        return homeUserRepository.findWithRegionById(userId)
                .map(User::getRegion)
                .orElse(null);
    }

    private List<DisabilityType> getUserDisabilityTypes(Long userId) {
        return homeUserRepository.findWithChildDisabilitiesById(userId)
                .map(user -> user.getDisabilityTypes().stream()
                        .map(d -> DisabilityType.valueOf(d.name()))
                        .toList())
                .orElse(List.of());
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
