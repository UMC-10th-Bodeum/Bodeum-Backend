package com.bodeum.domain.info.service;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.dto.response.InfoItemDetailResponse;
import com.bodeum.domain.info.dto.response.InfoItemPageResponse;
import com.bodeum.domain.info.dto.response.InfoItemResponse;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoItemQueryService {

    private final InfoItemRepository infoItemRepository;
    private final InfoCategoryRepository infoCategoryRepository;
    private final UserRepository userRepository;

    /**
     * 1. 메인 정보 목록 조회 / 검색 API
     */
    public InfoItemPageResponse getInfoItems(
            Long userId,
            InfoItemSearchCondition condition,
            Pageable pageable
    ) {
        // regionLevel1 조건이 들어오지 않고, 로그인한 유저인 경우 기본 지역 세팅
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

// --- 선택된 카테고리 정보 메타데이터 구성 ---
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
     * 2. 정보 상세 조회 API (스크랩 기능은 차후 구현 예정)
     */
    @Transactional
    public InfoItemDetailResponse getInfoItemDetail(Long userId, Long infoItemId) {
        // 1) 정보 아이템 조회 (없으면 404 예외)
        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));

        // 2) 조회수 1 증가
        infoItem.incrementViewCount();

        // 3) 스크랩 여부 (차후 구현 시 실제 스크랩 레포지토리 조회로 변경 예정)
        boolean isScrapped = false;

        // 4) 요일별 운영시간 매핑 (기본 빈 리스트 전달)
        List<InfoItemDetailResponse.BusinessHourDto> businessHours = List.of();

        return InfoItemDetailResponse.of(infoItem, isScrapped, businessHours);
    }
}