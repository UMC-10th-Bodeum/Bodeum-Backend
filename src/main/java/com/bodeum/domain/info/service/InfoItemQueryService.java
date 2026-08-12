package com.bodeum.domain.info.service;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.dto.request.KakaoMapUrlRequest;
import com.bodeum.domain.info.dto.response.InfoItemDetailResponse;
import com.bodeum.domain.info.dto.response.InfoItemPageResponse;
import com.bodeum.domain.info.dto.response.InfoItemResponse;
import com.bodeum.domain.info.dto.response.InfoItemShareResponse;
import com.bodeum.domain.info.dto.response.KakaoMapUrlResponse;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.*;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.enums.InterestCategory;
import com.bodeum.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoItemQueryService {

    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final UserRepository userRepository;
    private final InfoScrapRepository infoScrapRepository;
    private final InfoItemTagRepository infoItemTagRepository;

    @Value("${bodeum.share.base-url}")
    private String shareBaseUrl;

    /**
     * 1. 메인 정보 목록 조회 / 검색 API
     */
    public InfoItemPageResponse getInfoItems(
            Long userId,
            InfoItemSearchCondition condition,
            Pageable pageable
    ) {
        if (condition.subCategory() != null) {
            InfoCategory category = infoCategoryRepository.findById(condition.subCategory()).orElse(null);

            if (category != null && category.getSubCategory() != null
                    && category.getSubCategory().isRecommendationCategory()) {
                List<InfoItemResponse> recommendedList = getRecommendedInfoItems(userId, category.getMainCategory());

                // CodeRabbit 큰 offset 방어 로직
                long offset = pageable.getOffset();
                List<InfoItemResponse> pagedList;

                if (offset >= recommendedList.size()) {
                    pagedList = List.of();
                } else {
                    int start = (int) offset;
                    int end = Math.min(start + pageable.getPageSize(), recommendedList.size());
                    pagedList = recommendedList.subList(start, end);
                }

                Page<InfoItemResponse> recommendedPage = new PageImpl<>(
                        pagedList,
                        pageable,
                        recommendedList.size()
                );

                return InfoItemPageResponse.of(
                        category.getMainCategory(),
                        category.getMainCategoryKo(),
                        category.getId(),
                        category.getSubCategory().name(),
                        category.getSubCategoryKo(),
                        recommendedPage
                );
            }
        }

        if (condition.category() == null && condition.subCategory() == null) {
            condition = condition.withCategory(MainCategory.INSTITUTION);
        }

        if (!StringUtils.hasText(condition.regionLevel1())) {
            if (userId != null) {
                User loginUser = userRepository.findByIdWithGuardianProfileAndRegion(userId)
                        .orElseGet(() -> userRepository.findById(userId).orElse(null));

                if (loginUser != null && loginUser.getRegion() != null) {
                    Region userRegion = loginUser.getRegion();
                    condition = condition.withUserRegion(
                            userRegion.getRegionLevel1(),
                            userRegion.getRegionLevel2()
                    );
                }
            }
        }

        Page<InfoItem> infoItems = infoItemRepository.searchInfoItems(condition, pageable);

        // ★ 목록 내 Item들의 ID 추출 후 N+1 방지를 위해 IN 쿼리로 태그 배치 조회
        List<Long> itemIds = infoItems.stream().map(InfoItem::getId).toList();
        Map<Long, List<String>> tagMap = infoItemTagRepository.findAllByInfoItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        itemTag -> itemTag.getInfoItem().getId(),
                        Collectors.mapping(itemTag -> itemTag.getInfoTag().getName(), Collectors.toList())
                ));

        // InfoItemResponse 생성 시 매핑된 태그 리스트 전달
        Page<InfoItemResponse> itemResponses = infoItems.map(item ->
                InfoItemResponse.of(item, tagMap.getOrDefault(item.getId(), List.of()))
        );

        MainCategory selectedMainCategory = condition.category();
        String selectedMainCategoryKo = null;
        Long selectedSubCategoryId = condition.subCategory();
        String selectedSubCategory = null;
        String selectedSubCategoryKo = null;

        if (selectedSubCategoryId != null) {
            InfoCategory category = infoCategoryRepository.findById(selectedSubCategoryId).orElse(null);
            if (category != null) {
                selectedMainCategory = category.getMainCategory();
                selectedMainCategoryKo = category.getMainCategoryKo();
                selectedSubCategory = category.getSubCategory().name();
                selectedSubCategoryKo = category.getSubCategoryKo();
            }
        } else if (selectedMainCategory != null) {
            selectedMainCategoryKo = infoCategoryRepository.findFirstByMainCategory(selectedMainCategory)
                    .map(InfoCategory::getMainCategoryKo)
                    .orElse(null);
        }

        return InfoItemPageResponse.of(
                selectedMainCategory,
                selectedMainCategoryKo,
                selectedSubCategoryId,
                selectedSubCategory,
                selectedSubCategoryKo,
                itemResponses
        );
    }

    /**
     * 2. 정보 상세 조회 API
     */
    @Transactional
    public InfoItemDetailResponse getInfoItemDetail(Long userId, Long infoItemId) {
        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND));

        infoItem.incrementViewCount();

        boolean isScrapped = false;
        if (userId != null) {
            isScrapped = infoScrapRepository.existsByUserIdAndInfoItemId(userId, infoItemId);
        }

        // 해당 아이템의 태그 목록 조회
        List<String> tags = infoItemTagRepository.findAllByInfoItem(infoItem).stream()
                .map(itemTag -> itemTag.getInfoTag().getName())
                .toList();

        return InfoItemDetailResponse.of(infoItem, isScrapped, tags);
    }

    /**
     * 3. 정보 공유 URL 생성 API
     */
    public InfoItemShareResponse getInfoItemShareUrl(Long infoItemId) {
        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_SHARE_LINK_NOT_FOUND));

        String shareUrl = UriComponentsBuilder.fromUriString(shareBaseUrl)
                .pathSegment("info", infoItem.getInfoCategory().getMainCategory().name(), String.valueOf(infoItem.getId()))
                .build()
                .toUriString();

        return InfoItemShareResponse.of(infoItem.getId(), shareUrl);
    }

    /**
     * 4. 카카오 지도 길찾기 URL 생성 API
     */
    public KakaoMapUrlResponse createKakaoMapUrl(KakaoMapUrlRequest request) {
        InfoItem infoItem = infoItemRepository.findById(request.infoItemId())
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND));

        String searchQuery = (infoItem.getName() != null && !infoItem.getName().isBlank())
                ? infoItem.getName()
                : infoItem.getAddress();

        String encodedQuery = UriUtils.encodePathSegment(searchQuery, StandardCharsets.UTF_8);
        String kakaoMapUrl = "https://map.kakao.com/link/search/" + encodedQuery;

        return KakaoMapUrlResponse.from(kakaoMapUrl);
    }

    /**
     * 온보딩 유저 맞춤 추천 목록 조회 API
     */
    public List<InfoItemResponse> getRecommendedInfoItems(Long userId, MainCategory mainCategory) {
        if (userId == null) {
            return List.of();
        }

        User loginUser = userRepository.findAiProfileById(userId).orElse(null);

        if (loginUser == null || !loginUser.isOnboardingCompleted()) {
            return List.of();
        }

        Region region = loginUser.getRegion();
        List<InterestCategory> interestCategories = loginUser.getInterestCategories();

        if (region == null || interestCategories.isEmpty()) {
            return List.of();
        }

        List<InfoItem> recommendedItems = infoItemRepository.findBySidoAndSigunguAndInterestInAndMainCategory(
                region.getRegionLevel1(),
                region.getRegionLevel2(),
                interestCategories,
                mainCategory
        );

        if (recommendedItems.isEmpty()) {
            return List.of();
        }

        List<Long> itemIds = recommendedItems.stream().map(InfoItem::getId).toList();
        Map<Long, List<String>> tagMap = infoItemTagRepository.findAllByInfoItemIdIn(itemIds).stream()
                .collect(Collectors.groupingBy(
                        itemTag -> itemTag.getInfoItem().getId(),
                        Collectors.mapping(itemTag -> itemTag.getInfoTag().getName(), Collectors.toList())
                ));

        return recommendedItems.stream()
                .map(item -> InfoItemResponse.of(item, tagMap.getOrDefault(item.getId(), List.of())))
                .toList();
    }
}
