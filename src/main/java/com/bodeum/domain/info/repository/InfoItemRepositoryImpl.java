package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

// Q-Class 정적 임포트
import static com.bodeum.domain.info.entity.QInfoCategory.infoCategory;
import static com.bodeum.domain.info.entity.QInfoItem.infoItem;

@RequiredArgsConstructor
public class InfoItemRepositoryImpl implements InfoItemRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<InfoItem> searchInfoItems(InfoItemSearchCondition condition, Pageable pageable) {

        // 1. 실제 목록 조회 쿼리
        List<InfoItem> content = queryFactory
                .selectFrom(infoItem)
                // N+1 방지를 위한 fetchJoin
                .join(infoItem.infoCategory, infoCategory).fetchJoin()
                .where(
                        categoryEq(condition.category()),          // category
                        subCategoryEq(condition.subCategory()),    // subCategory
                        regionLevel1Eq(condition.regionLevel1()),   // regionLevel1 (시/도)
                        regionLevel2Eq(condition.regionLevel2())    // regionLevel2 (구/군)
                )
                .orderBy(getSortOrder(condition.sort()))            // sort
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 2. 전체 개수(Count) 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(infoItem.count())
                .from(infoItem)
                .join(infoItem.infoCategory, infoCategory)
                .where(
                        categoryEq(condition.category()),
                        subCategoryEq(condition.subCategory()),
                        regionLevel1Eq(condition.regionLevel1()),
                        regionLevel2Eq(condition.regionLevel2())
                );

        // 3. 페이징 최적화 적용 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 동적 조건 조립용 헬퍼 메서드 ---

    private BooleanExpression categoryEq(MainCategory category) {
        return category != null ? infoCategory.mainCategory.eq(category) : null;
    }

    private BooleanExpression subCategoryEq(Long subCategoryId) {
        if (subCategoryId == null) {
            return null; // 조건이 없으면 무시
        }
        return infoCategory.id.eq(subCategoryId);
    }

    private BooleanExpression regionLevel1Eq(String regionLevel1) {
        return StringUtils.hasText(regionLevel1) ? infoItem.sido.eq(regionLevel1) : null;
    }

    private BooleanExpression regionLevel2Eq(String regionLevel2) {
        return StringUtils.hasText(regionLevel2) ? infoItem.sigungu.eq(regionLevel2) : null;
    }

    // --- 동적 정렬 헬퍼 메서드 ---
    private OrderSpecifier<?> getSortOrder(String sort) {
        if (sort == null || sort.isBlank()) {
            return infoItem.viewCount.desc(); // 기본값: 조회수순(view)
        }

        return switch (sort.toLowerCase()) {
            case "scrap" -> infoItem.scrapCount.desc();
            case "review" -> infoItem.reviewCount.desc();
            case "view" -> infoItem.viewCount.desc();
            default -> infoItem.viewCount.desc(); // 기본 정렬 view
        };
    }
}