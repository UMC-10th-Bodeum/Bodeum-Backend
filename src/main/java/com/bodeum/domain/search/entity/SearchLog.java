package com.bodeum.domain.search.entity;

import com.bodeum.domain.search.enums.SearchType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "search_log")
public class SearchLog extends BaseCreatedEntity {

    public static final int KEYWORD_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "search_log_id")
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "keyword", nullable = false, length = KEYWORD_MAX_LENGTH)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false)
    private SearchType searchType;

    @Column(name = "result_count", nullable = false)
    private Long resultCount;

    @Builder
    private SearchLog(Long userId, String keyword, SearchType searchType, Long resultCount) {
        this.userId = userId;
        this.keyword = keyword;
        this.searchType = searchType;
        this.resultCount = resultCount;
    }

    public static SearchLog create(Long userId, String keyword, SearchType searchType, Long resultCount) {
        return SearchLog.builder()
                .userId(userId)
                .keyword(keyword)
                .searchType(searchType)
                .resultCount(resultCount)
                .build();
    }
}