package com.bodeum.domain.news.service;

import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.infrastructure.openapi.PublicDataNewsCollector;
import com.bodeum.domain.news.repository.NewsCategoryRepository;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsSourceRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.exception.RegionErrorCode;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NewsPublicDataSyncService {

    private final List<PublicDataNewsCollector> collectors;
    private final NewsRepository newsRepository;
    private final NewsCategoryRepository newsCategoryRepository;
    private final NewsSourceRepository newsSourceRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public NewsSyncResult sync() {
        Map<NewsCategoryCode, NewsCategory> categories = new HashMap<>();
        Map<String, Region> regions = loadRegions();
        int fetched = 0;
        int created = 0;
        int updated = 0;

        for (PublicDataNewsCollector collector : collectors) {
            NewsSource source = findOrCreateSource(collector);
            List<NewsCandidate> candidates = collector.collect(source);
            Map<String, News> newsByExternalItemId = loadNewsByExternalItemId(source, candidates);
            fetched += candidates.size();

            for (NewsCandidate candidate : candidates) {
                NewsCategory category = categories.computeIfAbsent(
                        candidate.categoryCode(),
                        this::findOrCreateCategory
                );
                Long regionId = resolveRegion(candidate.regionName(), regions);
                News news = newsByExternalItemId.get(candidate.externalItemId());

                if (news == null) {
                    News createdNews = newsRepository.save(
                            News.create(category, source, regionId, candidate)
                    );
                    newsByExternalItemId.put(candidate.externalItemId(), createdNews);
                    created++;
                } else {
                    news.updateCollectedData(category, source, regionId, candidate);
                    updated++;
                }
            }

            source.updateLastSyncedAt(Instant.now());
        }

        return new NewsSyncResult(fetched, created, updated);
    }

    private Map<String, News> loadNewsByExternalItemId(
            NewsSource source,
            List<NewsCandidate> candidates
    ) {
        List<String> externalItemIds = candidates.stream()
                .map(NewsCandidate::externalItemId)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        Map<String, News> newsByExternalItemId = new HashMap<>();
        if (externalItemIds.isEmpty()) {
            return newsByExternalItemId;
        }

        newsRepository.findAllByNewsSourceAndExternalItemIdIn(source, externalItemIds)
                .forEach(news -> newsByExternalItemId.put(news.getExternalItemId(), news));
        return newsByExternalItemId;
    }

    private NewsSource findOrCreateSource(PublicDataNewsCollector collector) {
        return newsSourceRepository.findBySourceTypeAndName(collector.getSourceType(), collector.sourceName())
                .orElseGet(() -> newsSourceRepository.save(NewsSource.create(
                        collector.getSourceType(),
                        collector.sourceName(),
                        collector.sourceApiBaseUrl(),
                        collector.sourceListUrl()
                )));
    }

    private NewsCategory findOrCreateCategory(NewsCategoryCode code) {
        return newsCategoryRepository.findByNewsTypeAndName(code.getNewsType(), code.name())
                .orElseGet(() -> newsCategoryRepository.save(NewsCategory.create(code)));
    }

    private Map<String, Region> loadRegions() {
        Map<String, Region> regions = new HashMap<>();
        regionRepository.findAllByOrderByRegionLevel1AscRegionLevel2Asc()
                .forEach(region -> regions.put(region.getFullName(), region));
        return regions;
    }

    private Long resolveRegion(String regionName, Map<String, Region> regions) {
        if (!StringUtils.hasText(regionName)) {
            return null;
        }

        Region region = regions.get(regionName);
        if (region != null) {
            return region.getId();
        }

        throw new ProjectException(RegionErrorCode.REGION_NOT_FOUND);
    }

    public record NewsSyncResult(int fetched, int created, int updated) {
    }
}
