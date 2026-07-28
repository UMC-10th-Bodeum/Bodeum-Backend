package com.bodeum.domain.search.service;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.search.dto.response.InfoSearchResponse;
import com.bodeum.domain.search.dto.response.SearchHistoryResponse;
import com.bodeum.domain.search.entity.SearchLog;
import com.bodeum.domain.search.enums.SearchType;
import com.bodeum.domain.search.exception.SearchErrorCode;
import com.bodeum.domain.search.exception.SearchException;
import com.bodeum.domain.search.repository.SearchInfoItemRepository;
import com.bodeum.domain.search.repository.SearchLogRepository;
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

    private final SearchInfoItemRepository searchInfoItemRepository;
    private final SearchLogRepository searchLogRepository;

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
}
