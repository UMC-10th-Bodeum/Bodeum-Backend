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
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.repository.InfoScrapRepository;
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
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoItemQueryService {

    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final UserRepository userRepository;
    private final InfoScrapRepository infoScrapRepository;

    @Value("${bodeum.share.base-url}")
    private String shareBaseUrl;

    /**
     * 1. 메인 정보 목록 조회 / 검색 API
     * - 기본 메인 카테고리: 클라이언트 요청이 없으면 '기관(INSTITUTION)' 기본 적용
     * - 지역 자동 적용:
     *   - 클라이언트가 regionLevel1을 지정하지 않고 + 로그인 유저인 경우 -> 관심 지역 적용
     *   - 비로그인 유저 또는 지역 미지정 시 -> 전체 지역 조회
     */
    public InfoItemPageResponse getInfoItems(
            Long userId,
            InfoItemSearchCondition condition,
            Pageable pageable
    ) {
        // [수정 포인트] 메인 카테고리 및 서브 카테고리가 모두 지정되지 않은 경우, 기본값 '기관(INSTITUTION)' 설정
        if (condition.category() == null && condition.subCategory() == null) {
            condition = condition.withCategory(MainCategory.INSTITUTION);
        }

        // 클라이언트에서 직접 지정한 지역 정보가 없는 경우
        if (!StringUtils.hasText(condition.regionLevel1())) {
            // 로그인 유저인 경우 관심 지역 가져오기
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
     * - 비로그인 유저: isScrapped = false
     * - 로그인 유저: existsByUserIdAndInfoItemId 로 쿼리 1번에 스크랩 유무 즉시 판단
     */
    @Transactional
    public InfoItemDetailResponse getInfoItemDetail(Long userId, Long infoItemId) {
        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND));

        // 조회수 증가
        infoItem.incrementViewCount();

        // 로그인 유저인 경우 추가 User 조회 쿼리 없이 즉시 스크랩 여부 확인
        boolean isScrapped = false;
        if (userId != null) {
            isScrapped = infoScrapRepository.existsByUserIdAndInfoItemId(userId, infoItemId);
        }

        List<InfoItemDetailResponse.BusinessHourDto> businessHours = List.of();

        return InfoItemDetailResponse.of(infoItem, isScrapped, businessHours);
    }

    /**
     * 3. 정보 공유 링크 조회 API
     */
    public InfoItemShareResponse getInfoItemShareUrl(Long infoItemId) {
        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_SHARE_LINK_NOT_FOUND));

        String shareUrl = UriComponentsBuilder.fromUriString(shareBaseUrl)
                .pathSegment("info", String.valueOf(infoItem.getId()))
                .build()
                .toUriString();

        return InfoItemShareResponse.of(infoItem.getId(), shareUrl);
    }

    /**
     * 4. 카카오지도 URL 생성 API
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
}