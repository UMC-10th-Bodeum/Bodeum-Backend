package com.bodeum.domain.mypage.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsScrap;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record MyScrapListResponse(
        long totalCount,
        List<InfoScrapItem> infoScraps,
        List<NewsScrapItem> newsScraps,
        List<PostScrapItem> postScraps
) {

    public static MyScrapListResponse of(
            List<InfoScrap> infoScraps,
            List<NewsScrap> newsScraps,
            List<PostScrap> postScraps
    ) {
        List<InfoScrapItem> infoScrapItems = infoScraps.stream()
                .map(InfoScrapItem::from)
                .toList();

        List<NewsScrapItem> newsScrapItems = newsScraps.stream()
                .map(NewsScrapItem::from)
                .toList();

        List<PostScrapItem> postScrapItems = postScraps.stream()
                .map(PostScrapItem::from)
                .toList();

        return new MyScrapListResponse(
                infoScrapItems.size() + newsScrapItems.size() + postScrapItems.size(),
                infoScrapItems,
                newsScrapItems,
                postScrapItems
        );
    }

    public record InfoScrapItem(
            Long scrapId,
            Long infoItemId,
            MainCategory mainCategory,
            String mainCategoryKo,
            String subCategory,
            String subCategoryKo,
            String name,
            String introduction,
            String address,
            String sido,
            String sigungu,
            String phone,
            String homepageUrl,
            Instant scrappedAt
    ) {

        private static InfoScrapItem from(InfoScrap scrap) {
            InfoItem infoItem = scrap.getInfoItem();
            InfoCategory infoCategory = infoItem.getInfoCategory();

            return new InfoScrapItem(
                    scrap.getId(),
                    infoItem.getId(),
                    infoCategory.getMainCategory(),
                    infoCategory.getMainCategoryKo(),
                    infoCategory.getSubCategory(),
                    infoCategory.getSubCategoryKo(),
                    infoItem.getName(),
                    infoItem.getIntroduction(),
                    infoItem.getAddress(),
                    infoItem.getSido(),
                    infoItem.getSigungu(),
                    infoItem.getPhone(),
                    infoItem.getHomepageUrl(),
                    scrap.getCreatedAt()
            );
        }
    }

    public record NewsScrapItem(
            Long scrapId,
            Long newsId,
            String title,
            String summary,
            String sourceName,
            String originalUrl,
            String thumbnailUrl,
            NewsType newsType,
            RecruitmentStatus recruitmentStatus,
            LocalDateTime publishedAt,
            Instant scrappedAt
    ) {

        private static NewsScrapItem from(NewsScrap scrap) {
            News news = scrap.getNews();

            return new NewsScrapItem(
                    scrap.getId(),
                    news.getId(),
                    news.getTitle(),
                    news.getSummary(),
                    news.getSourceName(),
                    news.getOriginalUrl(),
                    news.getThumbnailUrl(),
                    news.getNewsType(),
                    news.getRecruitmentStatus(),
                    news.getPublishedAt(),
                    scrap.getCreatedAt()
            );
        }
    }

    public record PostScrapItem(
            Long scrapId,
            Long postId,
            PostBoardType boardType,
            PostAnonymityType anonymityType,
            String title,
            String content,
            boolean isQuestion,
            int viewCount,
            int likeCount,
            int commentCount,
            int scrapCount,
            Instant createdAt,
            Instant updatedAt,
            Instant scrappedAt
    ) {

        private static PostScrapItem from(PostScrap scrap) {
            Post post = scrap.getPost();

            return new PostScrapItem(
                    scrap.getId(),
                    post.getId(),
                    post.getBoardType(),
                    post.getAnonymityType(),
                    post.getTitle(),
                    post.getContent(),
                    post.isQuestion(),
                    post.getViewCount(),
                    post.getLikeCount(),
                    post.getCommentCount(),
                    post.getScrapCount(),
                    post.getCreatedAt(),
                    post.getUpdatedAt(),
                    scrap.getCreatedAt()
            );
        }
    }
}
