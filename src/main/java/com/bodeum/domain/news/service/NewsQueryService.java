package com.bodeum.domain.news.service;

import com.bodeum.domain.news.dto.NewsSort;
import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.dto.response.NewsDetailResponse;
import com.bodeum.domain.news.dto.response.NewsListItemResponse;
import com.bodeum.domain.news.dto.response.NewsListResponse;
import com.bodeum.domain.news.dto.response.NewsSearchSuggestionsResponse;
import com.bodeum.domain.news.dto.response.RelatedRecruitingNewsResponse;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsScrapRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.domain.region.service.RegionService;
import com.bodeum.domain.region.service.ResolvedRegionFilter;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NewsQueryService {

    private static final List<Long> NO_REGION_IDS = List.of(-1L);

    private final NewsRepository newsRepository;
    private final NewsScrapRepository newsScrapRepository;
    private final RegionRepository regionRepository;
    private final RegionService regionService;

    @Transactional(readOnly = true)
    public NewsListResponse getNews(
            int page,
            int size,
            NewsSort sort,
            Long regionId,
            String regionLevel1,
            NewsCategoryCode category,
            NewsStatus status
    ) {
        ResolvedRegionFilter regionFilter = regionService.resolveFilter(regionId, regionLevel1);
        if (regionFilter.applied() && regionFilter.regionIds().isEmpty()) {
            return NewsListResponse.empty(page, size);
        }
        List<Long> regionIds = regionFilter.regionIds().isEmpty()
                ? NO_REGION_IDS
                : regionFilter.regionIds();

        PageRequest pageable = PageRequest.of(
                page,
                size,
                resolveSort(sort)
        );
        Page<News> result = newsRepository.findVisibleNews(
                category == null ? null : category.name(),
                status == null ? null : status.toEntity(),
                regionFilter.applied(),
                regionIds,
                pageable
        );

        return toListResponse(result);
    }

    @Transactional(readOnly = true)
    public NewsListResponse searchNews(
            String keyword,
            int page,
            int size,
            NewsSort sort,
            Long regionId,
            String regionLevel1,
            NewsCategoryCode category,
            NewsStatus status
    ) {
        ResolvedRegionFilter regionFilter = regionService.resolveFilter(regionId, regionLevel1);
        if (regionFilter.applied() && regionFilter.regionIds().isEmpty()) {
            return NewsListResponse.empty(page, size);
        }
        List<Long> regionIds = regionFilter.regionIds().isEmpty()
                ? NO_REGION_IDS
                : regionFilter.regionIds();

        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        List<Long> keywordRegionIds = resolveRegionIds(normalizedKeyword);
        PageRequest pageable = PageRequest.of(
                page,
                size,
                resolveSort(sort)
        );
        Page<News> result = newsRepository.searchVisibleNews(
                "%" + normalizedKeyword + "%",
                !keywordRegionIds.isEmpty(),
                keywordRegionIds.isEmpty() ? NO_REGION_IDS : keywordRegionIds,
                category == null ? null : category.name(),
                status == null ? null : status.toEntity(),
                regionFilter.applied(),
                regionIds,
                pageable
        );

        return toListResponse(result);
    }

    @Transactional(readOnly = true)
    public NewsSearchSuggestionsResponse getSearchSuggestions(String keyword, int size) {
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalizedKeyword.codePointCount(0, normalizedKeyword.length()) < 2) {
            throw new ProjectException(GeneralErrorCode.BAD_REQUEST);
        }
        List<String> titles = new ArrayList<>(
                newsRepository.findTitleSuggestionsStartingWith(
                        normalizedKeyword,
                        PageRequest.of(0, size)
                )
        );

        int remainingSize = size - titles.size();
        if (remainingSize > 0) {
            titles.addAll(newsRepository.findTitleSuggestionsContaining(
                    normalizedKeyword,
                    PageRequest.of(0, remainingSize)
            ));
        }

        return NewsSearchSuggestionsResponse.fromTitles(titles);
    }

    private NewsListResponse toListResponse(Page<News> result) {
        Map<Long, String> regionNames = resolveRegionNames(result.getContent());
        List<NewsListItemResponse> items = result.getContent().stream()
                .map(news -> NewsListItemResponse.of(news, regionNames.get(news.getRegionId())))
                .toList();

        return new NewsListResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Transactional
    public NewsDetailResponse getNewsDetail(Long userId, Long newsId) {
        if (newsRepository.incrementViewCount(newsId) == 0) {
            throw new ProjectException(GeneralErrorCode.NOT_FOUND);
        }

        News news = newsRepository.findVisibleById(newsId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
        String regionName = news.getRegionId() == null
                ? null
                : regionRepository.findById(news.getRegionId())
                        .map(Region::getFullName)
                        .orElse(null);
        boolean scrapped = userId != null
                && newsScrapRepository.existsByNewsIdAndUserId(newsId, userId);

        return NewsDetailResponse.of(news, regionName, scrapped);
    }

    @Transactional(readOnly = true)
    public List<RelatedRecruitingNewsResponse> getRelatedRecruitingNews(
            Long newsId,
            int size
    ) {
        News currentNews = newsRepository.findVisibleById(newsId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
        Long regionId = currentNews.getRegionId();
        if (regionId == null) {
            return List.of();
        }

        Region currentRegion = regionRepository.findById(regionId)
                .orElse(null);
        if (currentRegion == null) {
            return List.of();
        }

        List<Region> cityRegions = resolveCityRegions(currentRegion);
        List<Long> cityRegionIds = cityRegions.stream()
                .map(Region::getId)
                .toList();
        Map<Long, String> regionNames = cityRegions.stream()
                .collect(Collectors.toMap(Region::getId, Region::getFullName));
        PageRequest pageable = PageRequest.of(
                0,
                size,
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
        );

        return newsRepository.findRelatedRecruitingNews(
                        newsId,
                        cityRegionIds,
                        RecruitmentStatus.OPEN,
                        pageable
                ).stream()
                .map(news -> RelatedRecruitingNewsResponse.of(
                        news,
                        regionNames.get(news.getRegionId())
                ))
                .toList();
    }

    private List<Region> resolveCityRegions(Region currentRegion) {
        boolean cityIsRegionLevel1 = currentRegion.getRegionLevel1().endsWith("시");
        String municipality = primaryMunicipality(currentRegion.getRegionLevel2());

        return regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc().stream()
                .filter(candidate -> currentRegion.getRegionLevel1()
                        .equals(candidate.getRegionLevel1()))
                .filter(candidate -> cityIsRegionLevel1
                        || municipality.equals(primaryMunicipality(candidate.getRegionLevel2())))
                .toList();
    }

    private String primaryMunicipality(String regionLevel2) {
        return regionLevel2.trim().split("\\s+", 2)[0];
    }

    private List<Long> resolveRegionIds(String region) {
        if (!StringUtils.hasText(region)) {
            return List.of();
        }

        String keyword = region.trim().toLowerCase(Locale.ROOT);
        return regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc().stream()
                .filter(candidate -> contains(candidate.getRegionLevel1(), keyword)
                        || contains(candidate.getRegionLevel2(), keyword)
                        || contains(candidate.getFullName(), keyword))
                .map(Region::getId)
                .toList();
    }

    private Sort resolveSort(NewsSort sort) {
        NewsSort resolvedSort = sort == null ? NewsSort.VIEW : sort;
        Sort.Order primaryOrder = switch (resolvedSort) {
            case VIEW -> Sort.Order.desc("viewCount");
            case SCRAP -> Sort.Order.desc("scrapCount");
        };
        return Sort.by(
                primaryOrder,
                Sort.Order.desc("publishedAt"),
                Sort.Order.desc("id")
        );
    }

    private Map<Long, String> resolveRegionNames(Collection<News> newsItems) {
        List<Long> ids = newsItems.stream()
                .map(News::getRegionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return regionRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Region::getId, Region::getFullName, (left, right) -> left));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

}
