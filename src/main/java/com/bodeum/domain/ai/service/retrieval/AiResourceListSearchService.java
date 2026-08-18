package com.bodeum.domain.ai.service.retrieval;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정보 탭의 기관 데이터를 조건과 지역에 맞게 조회하고,
 * 추가 결과에서 이전에 안내한 기관을 제외한다.
 */
@Service
@RequiredArgsConstructor
public class AiResourceListSearchService {

    private static final int CANDIDATE_MULTIPLIER = 4;

    private final InfoItemRepository infoItemRepository;
    private final AiAnswerEvidenceService evidenceService;

    @Value("${bodeum.ai.rag.top-k:5}")
    private int defaultResultCount;

    @Value("${bodeum.ai.result.max-count:10}")
    private int maxResultCount;

    @Transactional(readOnly = true)
    public List<AiReferenceDocument> retrieve(
            AiUserProfile profile,
            AiSearchScope searchScope,
            Integer requestedResultCount,
            Set<AiSourceKey> excludedSources,
            Set<String> excludedIdentityKeys
    ) {
        if (profile == null || profile.infoSubCategory() == null) {
            return List.of();
        }
        int requestedCount = requestedResultCount == null
                ? defaultResultCount
                : requestedResultCount;
        int resultCount = Math.min(Math.max(1, requestedCount), maxResultCount);
        int candidateCount = Math.max(resultCount * CANDIDATE_MULTIPLIER, maxResultCount);
        PageRequest page = PageRequest.of(0, candidateCount);

        LinkedHashMap<Long, InfoItem> candidates = new LinkedHashMap<>();
        if (hasText(profile.regionLevel1()) && hasText(profile.regionLevel2())) {
            add(candidates, infoItemRepository.findRehabCentersByRegion(
                    profile.regionLevel1(), profile.regionLevel2(),
                    profile.infoSubCategory(), page));
        }
        if (searchScope != AiSearchScope.LOCAL_ONLY) {
            if (hasText(profile.regionLevel1())) {
                add(candidates, infoItemRepository.findByRegionLevel1AndSubCategory(
                        profile.regionLevel1(), profile.infoSubCategory(), page));
            }
            add(candidates, infoItemRepository.findBySubCategory(
                    profile.infoSubCategory(), page));
        }

        Set<AiSourceKey> safeExcludedSources = excludedSources == null
                ? Set.of() : excludedSources;
        Set<String> safeExcludedIdentities = excludedIdentityKeys == null
                ? Set.of() : excludedIdentityKeys;
        return candidates.values().stream()
                .map(this::toReferenceDocument)
                .filter(document -> !safeExcludedSources.contains(
                        new AiSourceKey(document.sourceType(), document.sourceId())))
                .filter(document -> evidenceService.documentIdentityKeys(document).stream()
                        .noneMatch(safeExcludedIdentities::contains))
                .limit(resultCount)
                .toList();
    }

    private void add(LinkedHashMap<Long, InfoItem> candidates, List<InfoItem> items) {
        items.forEach(item -> candidates.putIfAbsent(item.getId(), item));
    }

    private AiReferenceDocument toReferenceDocument(InfoItem item) {
        InfoCategory category = item.getInfoCategory();
        String content = lines(
                line("정보명", item.getName()),
                line("대분류", category.getMainCategoryKo()),
                line("세부 분류", category.getSubCategoryKo()),
                line("소개", item.getIntroduction()),
                line("주소", item.getAddress()),
                line("지역", region(item)),
                line("전화번호", item.getPhone()),
                line("홈페이지", item.getHomepageUrl())
        );
        return new AiReferenceDocument(
                "INFO-%d-0".formatted(item.getId()), content,
                AiResponseSourceType.INFO, item.getId(), item.getName(),
                item.getHomepageUrl(), item.getUpdatedAt());
    }

    private String region(InfoItem item) {
        return java.util.stream.Stream.of(item.getSido(), item.getSigungu())
                .filter(this::hasText)
                .reduce((first, second) -> first + " " + second)
                .orElse(null);
    }

    private String line(String label, Object value) {
        return value == null || value.toString().isBlank() ? "" : label + ": " + value;
    }

    private String lines(String... lines) {
        List<String> present = new ArrayList<>();
        for (String line : lines) {
            if (!line.isBlank()) {
                present.add(line);
            }
        }
        return String.join("\n", present);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
