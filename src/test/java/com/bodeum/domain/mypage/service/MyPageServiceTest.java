package com.bodeum.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.mypage.dto.response.MyPageProfileResponse;
import com.bodeum.domain.mypage.dto.response.MyScrapListResponse;
import com.bodeum.domain.mypage.repository.MyPageCommentRepository;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostRepository;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsScrap;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private MyPageInfoScrapRepository infoScrapRepository;

    @Mock
    private MyPageNewsScrapRepository newsScrapRepository;

    @Mock
    private MyPagePostRepository postRepository;

    @Mock
    private MyPageCommentRepository commentRepository;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    void getProfileReturnsProfileAndActivitySummary() {
        UserProfileResponse profile =
                new UserProfileResponse(
                        1L,
                        "민준맘",
                        null,
                        230,
                        3,
                        "꽃",
                        "정성스러운 답변과 경험 공유로 "
                                + "커뮤니티에 본격적인 신뢰의 결실을 피워내는 단계입니다.",
                        new UserProfileResponse.ChildProfile(
                                null,
                                null,
                                List.of()
                        ),
                        null,
                        List.of(),
                        null,
                        null,
                        null,
                        "민준맘",
                        null,
                        null
                );

        given(userService.getProfile(1L))
                .willReturn(profile);

        given(infoScrapRepository.countByUserId(1L))
                .willReturn(3L);

        given(newsScrapRepository.countVisibleByUserId(1L))
                .willReturn(2L);

        given(
                postRepository.countVisibleByUserId(
                        1L,
                        PostStatus.ACTIVE
                )
        ).willReturn(4L);

        given(
                commentRepository.countVisibleByUserId(
                        1L,
                        CommentStatus.ACTIVE,
                        PostStatus.ACTIVE
                )
        ).willReturn(7L);

        MyPageProfileResponse response =
                myPageService.getProfile(1L);

        assertThat(response.userId())
                .isEqualTo(1L);

        assertThat(response.nickname())
                .isEqualTo("민준맘");

        assertThat(response.point())
                .isEqualTo(230);

        assertThat(response.activitySummary().savedInfoCount())
                .isEqualTo(5L);

        assertThat(response.activitySummary().myPostCount())
                .isEqualTo(4L);

        assertThat(response.activitySummary().myCommentCount())
                .isEqualTo(7L);
    }

    @Test
    void getScrapsReturnsInfoAndNewsScraps() {
        User user = mock(User.class);

        InfoScrap infoScrap = mock(InfoScrap.class);
        InfoItem infoItem = mock(InfoItem.class);
        InfoCategory infoCategory = mock(InfoCategory.class);

        NewsScrap newsScrap = mock(NewsScrap.class);
        News news = mock(News.class);

        Instant infoScrappedAt =
                Instant.parse("2026-07-17T20:00:00Z");

        Instant newsScrappedAt =
                Instant.parse("2026-07-16T18:00:00Z");

        LocalDateTime publishedAt =
                LocalDateTime.of(
                        2026,
                        7,
                        15,
                        10,
                        30
                );

        given(userService.getCurrentUser(1L))
                .willReturn(user);

        given(
                infoScrapRepository
                        .findAllByUserIdOrderByCreatedAtDesc(1L)
        ).willReturn(List.of(infoScrap));

        given(
                newsScrapRepository
                        .findAllVisibleByUserIdOrderByCreatedAtDesc(1L)
        ).willReturn(List.of(newsScrap));

        given(infoScrap.getId())
                .willReturn(10L);

        given(infoScrap.getInfoItem())
                .willReturn(infoItem);

        given(infoScrap.getCreatedAt())
                .willReturn(infoScrappedAt);

        given(infoItem.getId())
                .willReturn(100L);

        given(infoItem.getInfoCategory())
                .willReturn(infoCategory);

        given(infoCategory.getMainCategory())
                .willReturn(MainCategory.WELFARE);

        given(infoCategory.getMainCategoryKo())
                .willReturn("복지");

        given(infoCategory.getSubCategory())
                .willReturn("WELFARE_CENTER");

        given(infoCategory.getSubCategoryKo())
                .willReturn("장애인복지관");

        given(infoItem.getName())
                .willReturn("성남시 장애인복지관");

        given(infoItem.getIntroduction())
                .willReturn("장애인 대상 복지 프로그램을 운영합니다.");

        given(infoItem.getAddress())
                .willReturn("경기도 성남시 분당구");

        given(infoItem.getSido())
                .willReturn("경기도");

        given(infoItem.getSigungu())
                .willReturn("성남시");

        given(infoItem.getPhone())
                .willReturn("031-123-4567");

        given(infoItem.getHomepageUrl())
                .willReturn("https://example.com");

        given(newsScrap.getId())
                .willReturn(20L);

        given(newsScrap.getNews())
                .willReturn(news);

        given(newsScrap.getCreatedAt())
                .willReturn(newsScrappedAt);

        given(news.getId())
                .willReturn(200L);

        given(news.getTitle())
                .willReturn("발달장애인 가족 지원 프로그램 모집");

        given(news.getSummary())
                .willReturn("가족 지원 프로그램 참여자를 모집합니다.");

        given(news.getSourceName())
                .willReturn("성남시청");

        given(news.getOriginalUrl())
                .willReturn("https://example.com/news/200");

        given(news.getThumbnailUrl())
                .willReturn("https://example.com/image.jpg");

        given(news.getNewsType())
                .willReturn(NewsType.ACTIVITY);

        given(news.getRecruitmentStatus())
                .willReturn(RecruitmentStatus.OPEN);

        given(news.getPublishedAt())
                .willReturn(publishedAt);

        MyScrapListResponse response =
                myPageService.getScraps(1L);

        assertThat(response.totalCount())
                .isEqualTo(2L);

        assertThat(response.infoScraps())
                .hasSize(1);

        assertThat(response.newsScraps())
                .hasSize(1);

        MyScrapListResponse.InfoScrapItem infoResponse =
                response.infoScraps().getFirst();

        assertThat(infoResponse.scrapId())
                .isEqualTo(10L);

        assertThat(infoResponse.infoItemId())
                .isEqualTo(100L);

        assertThat(infoResponse.mainCategory())
                .isEqualTo(MainCategory.WELFARE);

        assertThat(infoResponse.mainCategoryKo())
                .isEqualTo("복지");

        assertThat(infoResponse.subCategory())
                .isEqualTo("WELFARE_CENTER");

        assertThat(infoResponse.subCategoryKo())
                .isEqualTo("장애인복지관");

        assertThat(infoResponse.name())
                .isEqualTo("성남시 장애인복지관");

        assertThat(infoResponse.scrappedAt())
                .isEqualTo(infoScrappedAt);

        MyScrapListResponse.NewsScrapItem newsResponse =
                response.newsScraps().getFirst();

        assertThat(newsResponse.scrapId())
                .isEqualTo(20L);

        assertThat(newsResponse.newsId())
                .isEqualTo(200L);

        assertThat(newsResponse.title())
                .isEqualTo("발달장애인 가족 지원 프로그램 모집");

        assertThat(newsResponse.newsType())
                .isEqualTo(NewsType.ACTIVITY);

        assertThat(newsResponse.recruitmentStatus())
                .isEqualTo(RecruitmentStatus.OPEN);

        assertThat(newsResponse.publishedAt())
                .isEqualTo(publishedAt);

        assertThat(newsResponse.scrappedAt())
                .isEqualTo(newsScrappedAt);

        verify(userService).getCurrentUser(1L);
    }

    @Test
    void getScrapsReturnsEmptyListsWhenUserHasNoScraps() {
        User user = mock(User.class);

        given(userService.getCurrentUser(1L))
                .willReturn(user);

        given(
                infoScrapRepository
                        .findAllByUserIdOrderByCreatedAtDesc(1L)
        ).willReturn(List.of());

        given(
                newsScrapRepository
                        .findAllVisibleByUserIdOrderByCreatedAtDesc(1L)
        ).willReturn(List.of());

        MyScrapListResponse response =
                myPageService.getScraps(1L);

        assertThat(response.totalCount())
                .isZero();

        assertThat(response.infoScraps())
                .isEmpty();

        assertThat(response.newsScraps())
                .isEmpty();

        verify(userService).getCurrentUser(1L);
    }
}