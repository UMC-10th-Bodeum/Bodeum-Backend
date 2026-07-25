package com.bodeum.domain.news.service;

import com.bodeum.domain.news.dto.NewsStatus;
import com.bodeum.domain.news.dto.response.NewsDetailResponse;
import com.bodeum.domain.news.dto.response.NewsListItemResponse;
import com.bodeum.domain.news.dto.response.NewsListResponse;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsScrapRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Collection;
import java.util.List;
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

    @Transactional(readOnly = true)
    public NewsListResponse getNews(
            int page,
            int size,
            String region,
            String category,
            NewsStatus status
    ) {
        List<Long> regionIds = resolveRegionIds(region);
        boolean filterByRegion = StringUtils.hasText(region);
        if (filterByRegion && regionIds.isEmpty()) {
            return NewsListResponse.empty(page, size);
        }

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))
        );
        Page<News> result = newsRepository.findVisibleNews(
                normalize(category),
                status == null ? null : status.toEntity(),
                filterByRegion,
                regionIds.isEmpty() ? NO_REGION_IDS : regionIds,
                pageable
        );
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

    private List<Long> resolveRegionIds(String region) {
        if (!StringUtils.hasText(region)) {
            return List.of();
        }

        String keyword = region.trim().toLowerCase();
        return regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc().stream()
                .filter(candidate -> contains(candidate.getRegionLevel1(), keyword)
                        || contains(candidate.getRegionLevel2(), keyword)
                        || contains(candidate.getFullName(), keyword))
                .map(Region::getId)
                .toList();
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
        return value != null && value.toLowerCase().contains(keyword);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
