package com.bodeum.domain.info.service;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.dto.request.KakaoMapUrlRequest;
import com.bodeum.domain.info.dto.response.InfoItemDetailResponse;
import com.bodeum.domain.info.dto.response.InfoItemPageResponse;
import com.bodeum.domain.info.dto.response.InfoItemResponse;
import com.bodeum.domain.info.dto.response.KakaoMapUrlResponse;
import com.bodeum.domain.info.dto.response.InfoItemShareResponse;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoItemQueryService {

    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final UserRepository userRepository;

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
        if (!StringUtils.hasText(condition.regionLevel1()) && userId != null) {
            User loginUser = userRepository.findById(userId).orElse(null);

            if (loginUser != null && loginUser.getRegion() != null) {
                Region userRegion = loginUser.getRegion();
                condition = condition.withUserRegion(
                        userRegion.getRegionLevel1(),
                        userRegion.getRegionLevel2()
                );
            }
        }

        Page<InfoItem> infoItems = infoItemRepository.searchInfoItems(condition, pageable);
        Page<InfoItemResponse> itemResponses = infoItems.map(InfoItemResponse::from);

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
        List<InfoItemDetailResponse.BusinessHourDto> businessHours = List.of();

        return InfoItemDetailResponse.of(infoItem, isScrapped, businessHours);
    }

    /**
     * 4. 카카오지도 URL 생성 API
     */
    public KakaoMapUrlResponse createKakaoMapUrl(KakaoMapUrlRequest request) {
        // 도메인 예외 InfoException(INFO_ITEM_NOT_FOUND) 적용
        InfoItem infoItem = infoItemRepository.findById(request.infoItemId())
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND));

        // 장소명이 존재하면 장소명, 없으면 주소 기반 키워드 인코딩
        String searchQuery = (infoItem.getName() != null && !infoItem.getName().isBlank())
                ? infoItem.getName()
                : infoItem.getAddress();

        String encodedQuery = URLEncoder.encode(searchQuery, StandardCharsets.UTF_8);
        String kakaoMapUrl = "https://map.kakao.com/link/search/" + encodedQuery;

        return KakaoMapUrlResponse.from(kakaoMapUrl);
     * 3. 정보 공유 링크 조회 API
     */
    public InfoItemShareResponse getInfoItemShareUrl(Long infoItemId) {
        // 1) 공유 전용 예외 처리
        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_SHARE_LINK_NOT_FOUND));

        // 2) UriComponentsBuilder로 URL 정규화 (fromUriString 사용)
        String shareUrl = UriComponentsBuilder.fromUriString(shareBaseUrl)
                .pathSegment("info", String.valueOf(infoItem.getId()))
                .build()
                .toUriString();

        return InfoItemShareResponse.of(infoItem.getId(), shareUrl);
    }
}