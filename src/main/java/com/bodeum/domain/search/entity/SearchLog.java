package com.bodeum.domain.search.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "search_log")
public class SearchLog extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long searchLogId;

    // user_id는 NULL 허용 (비로그인 사용자도 검색 로그 기록)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SearchType searchType;

    @Column(nullable = false)
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