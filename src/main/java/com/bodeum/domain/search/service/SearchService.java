package com.bodeum.domain.search.service;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.search.dto.response.AutocompleteResponse;
import com.bodeum.domain.search.dto.response.InfoSearchResponse;
import com.bodeum.domain.search.dto.response.SearchHistoryResponse;
import com.bodeum.domain.search.entity.SearchLog;
import com.bodeum.domain.search.enums.SearchType;
import com.bodeum.domain.search.exception.SearchErrorCode;
import com.bodeum.domain.search.exception.SearchException;
import com.bodeum.domain.search.repository.SearchInfoItemRepository;
import com.bodeum.domain.search.repository.SearchLogRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private static final int HISTORY_LIMIT = 10;
    private static final int SEARCH_RESULT_LIMIT = 50;
    private static final int AUTOCOMPLETE_LIMIT = 10; // 자동완성 추천 최대 개수

    private final SearchInfoItemRepository searchInfoItemRepository;
    private final SearchLogRepository searchLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public InfoSearchResponse searchInfo(String keyword, Long userId) {
        if (keyword == null || keyword.length() < 2) {
            throw new SearchException(SearchErrorCode.KEYWORD_TOO_SHORT);
        }
        List<InfoItem> items = searchInfoItemRepository.findByNameContaining(keyword, PageRequest.of(0, SEARCH_RESULT_LIMIT));
        searchLogRepository.save(SearchLog.create(userId, keyword, SearchType.INFO, (long) items.size()));
        return InfoSearchResponse.of(items);
    }

    public SearchHistoryResponse getSearchHistory(Long userId) {
        List<String> keywords = searchLogRepository.findDistinctKeywordsByUserIdOrderByLatest(
                userId, PageRequest.of(0, HISTORY_LIMIT)
        );
        return new SearchHistoryResponse(keywords);
    }

    @Transactional
    public void deleteSearchHistory(Long userId, String keyword) {
        int deleted = searchLogRepository.deleteByUserIdAndKeyword(userId, keyword);
        if (deleted == 0) {
            throw new SearchException(SearchErrorCode.SEARCH_HISTORY_NOT_FOUND);
        }
    }

    public AutocompleteResponse getAutocomplete(String keyword, Long userId) {
        // 1. 공백/null 검증 -> 예외 발생
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new SearchException(SearchErrorCode.SEARCH_KEYWORD_BLANK);
        }

        String trimmedKeyword = keyword.trim();

        // 2. 최대 길이 검증 -> 예외 발생
        if (trimmedKeyword.length() > 50) {
            throw new SearchException(SearchErrorCode.SEARCH_KEYWORD_TOO_LONG);
        }

        // 3. 로그인 유저의 시/도(sido) 및 시/군/구(sigungu) 정보 추출
        String userSido = null;
        String userSigungu = null;

        if (userId != null) {
            User user = userRepository.findByIdWithGuardianProfileAndRegion(userId)
                    .orElseGet(() -> userRepository.findById(userId).orElse(null));
            if (user != null && user.getRegion() != null) {
                Region region = user.getRegion();
                userSido = region.getRegionLevel1();
                userSigungu = region.getRegionLevel2();
            }
        }
        // 4. 가중치 적용 자동완성 조회
        List<InfoItem> items = searchInfoItemRepository.findAutocompleteByKeyword(
                trimmedKeyword,
                userSido,
                userSigungu,
                PageRequest.of(0, AUTOCOMPLETE_LIMIT)
        );

        return AutocompleteResponse.from(items);
    }

    /**
     * 회원 탈퇴 시 해당 회원의 검색 기록을 모두 삭제한다.
     */
    @Transactional
    public void deleteUserSearchLogs(Long userId) {
        searchLogRepository.deleteByUserId(userId);
    }
}