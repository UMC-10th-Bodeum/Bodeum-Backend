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
    private final InfoOperatingHourRepository infoOperatingHourRepository;
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
        // ★ [추가된 로직] 프론트에서 isRecommended=true 파라미터를 보낸 경우
        if (Boolean.TRUE.equals(condition.isRecommended())) {
            List<InfoItemResponse> recommendedList = getRecommendedInfoItems(userId);

            // CodeRabbit 지적 사항 반영: Pageable 기준 메모리 내 subList 슬라이싱 처리
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), recommendedList.size());

            List<InfoItemResponse> pagedList = (start <= end)
                    ? recommendedList.subList(start, end)
                    : List.of();

            Page<InfoItemResponse> recommendedPage = new PageImpl<>(
                    pagedList,
                    pageable,
                    recommendedList.size()
            );

            MainCategory category = condition.category() != null ? condition.category() : MainCategory.INSTITUTION;
            String categoryKo = infoCategoryRepository.findFirstByMainCategory(category)
                    .map(InfoCategory::getMainCategoryKo)
                    .orElse("기관");

            return InfoItemPageResponse.of(
                    category,
                    categoryKo,
                    null,
                    "RECOMMEND",
                    "추천",
                    recommendedPage
            );
        }

        // ================= 기존 로직 100% 동일하게 유지 =================
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
                selectedSubCategory = category.getSubCategory();
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

        // 해당 아이템의 운영시간 목록 DB 조회 및 DTO 매핑
        List<InfoItemDetailResponse.BusinessHourDto> businessHours =
                infoOperatingHourRepository.findAllByInfoItem(infoItem).stream()
                        .map(hour -> new InfoItemDetailResponse.BusinessHourDto(
                                hour.getDayOfWeek() != null ? hour.getDayOfWeek().name() : null,
                                hour.getOpenTime() != null ? hour.getOpenTime().toString() : null,
                                hour.getCloseTime() != null ? hour.getCloseTime().toString() : null
                        ))
                        .toList();

        return InfoItemDetailResponse.of(infoItem, isScrapped, tags, businessHours);
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
    public List<InfoItemResponse> getRecommendedInfoItems(Long userId) {
        // 1. 비로그인 유저 -> 빈 리스트 (비노출)
        if (userId == null) {
            return List.of();
        }

        // 2. 로그인 유저 조회
        User loginUser = userRepository.findAiProfileById(userId).orElse(null);

        // 3. 유저가 없거나, 온보딩을 미완료했거나, 지역/관심사 정보가 없으면 빈 리스트 (비노출)
        if (loginUser == null || !loginUser.isOnboardingCompleted()) {
            return List.of();
        }

        Region region = loginUser.getRegion();
        List<InterestCategory> interestCategories = loginUser.getInterestCategories();

        if (region == null || interestCategories.isEmpty()) {
            return List.of();
        }

        // 4. 유저 지역(sido, sigungu) + 관심사 카테고리 기반 추천 아이템 DB 조회
        List<InfoItem> recommendedItems = infoItemRepository.findBySidoAndSigunguAndInterestIn(
                region.getRegionLevel1(), // 예: 서울특별시 / 경기도 등
                region.getRegionLevel2(), // 예: 강남구 / 수원시 등
                interestCategories
        );

        if (recommendedItems.isEmpty()) {
            return List.of();
        }

        // 5. N+1 방지를 위한 태그 배치 조회 및 DTO 변환
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