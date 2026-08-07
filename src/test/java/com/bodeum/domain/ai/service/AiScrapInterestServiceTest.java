package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.community.entity.Hashtag;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostHashtag;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.repository.PostHashtagRepository;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostScrapRepository;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsScrap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AiScrapInterestServiceTest {

    @Mock MyPageInfoScrapRepository infoScrapRepository;
    @Mock MyPageNewsScrapRepository newsScrapRepository;
    @Mock MyPagePostScrapRepository postScrapRepository;
    @Mock PostHashtagRepository postHashtagRepository;

    private AiScrapInterestService service;

    @BeforeEach
    void setUp() {
        service = new AiScrapInterestService(
                infoScrapRepository,
                newsScrapRepository,
                postScrapRepository,
                postHashtagRepository
        );
    }

    @Test
    void returnsRecentScrapTitlesAndCommunityTopics() {
        InfoScrap infoScrap = mock(InfoScrap.class);
        InfoItem infoItem = mock(InfoItem.class);
        when(infoScrap.getInfoItem()).thenReturn(infoItem);
        when(infoItem.getName()).thenReturn("수원시 발달재활서비스 제공기관");
        when(infoItem.getCategoryNames()).thenReturn(List.of("복지", "재활기관"));
        when(infoScrapRepository.findRecentByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(infoScrap));

        NewsScrap newsScrap = mock(NewsScrap.class);
        News news = mock(News.class);
        NewsCategory newsCategory = mock(NewsCategory.class);
        when(newsScrap.getNews()).thenReturn(news);
        when(news.getTitle()).thenReturn("2026년 발달재활서비스 신청 안내");
        when(news.getNewsCategory()).thenReturn(newsCategory);
        when(newsCategory.getLabel()).thenReturn("복지지원");
        when(newsScrapRepository.findRecentVisibleByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(newsScrap));

        PostScrap postScrap = mock(PostScrap.class);
        Post post = mock(Post.class);
        when(postScrap.getPost()).thenReturn(post);
        when(post.getId()).thenReturn(3L);
        when(post.getTitle()).thenReturn("특수학교 정보를 찾고 있어요");
        when(post.getBoardType()).thenReturn(PostBoardType.INFORMATION_QUESTION);
        when(postScrapRepository.findRecentVisibleByUserId(
                eq(1L), eq(PostStatus.ACTIVE), any(Pageable.class)
        )).thenReturn(List.of(postScrap));

        PostHashtag postHashtag = mock(PostHashtag.class);
        Hashtag hashtag = mock(Hashtag.class);
        when(postHashtag.getPost()).thenReturn(post);
        when(postHashtag.getHashtag()).thenReturn(hashtag);
        when(hashtag.getName()).thenReturn("특수학교");
        when(postHashtagRepository.findAllByPost_IdIn(List.of(3L)))
                .thenReturn(List.of(postHashtag));

        var result = service.findRecentInterests(1L);

        assertThat(result.infoTitles())
                .containsExactly("수원시 발달재활서비스 제공기관 "
                        + "(카테고리: 복지 > 재활기관)");
        assertThat(result.newsTitles())
                .containsExactly("2026년 발달재활서비스 신청 안내 "
                        + "(카테고리: 복지지원)");
        assertThat(result.communityTopics())
                .containsExactly("특수학교 정보를 찾고 있어요 "
                        + "(게시판: INFORMATION_QUESTION, 태그: 특수학교)");
        verify(infoScrapRepository).findRecentByUserId(
                eq(1L),
                org.mockito.ArgumentMatchers.argThat(pageable -> pageable.getPageSize() == 5)
        );
    }

    @Test
    void keepsTitlesWhenOptionalMetadataIsMissing() {
        InfoScrap infoScrap = mock(InfoScrap.class);
        InfoItem infoItem = mock(InfoItem.class);
        when(infoScrap.getInfoItem()).thenReturn(infoItem);
        when(infoItem.getName()).thenReturn("카테고리 없는 정보");
        when(infoItem.getCategoryNames()).thenReturn(List.of());
        when(infoScrapRepository.findRecentByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(infoScrap));

        NewsScrap newsScrap = mock(NewsScrap.class);
        News news = mock(News.class);
        when(newsScrap.getNews()).thenReturn(news);
        when(news.getTitle()).thenReturn("카테고리 없는 소식");
        when(news.getNewsCategory()).thenReturn(null);
        when(newsScrapRepository.findRecentVisibleByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(newsScrap));

        PostScrap postScrap = mock(PostScrap.class);
        Post post = mock(Post.class);
        when(postScrap.getPost()).thenReturn(post);
        when(post.getId()).thenReturn(4L);
        when(post.getTitle()).thenReturn("유형과 태그 없는 게시글");
        when(post.getBoardType()).thenReturn(null);
        when(postScrapRepository.findRecentVisibleByUserId(
                eq(1L), eq(PostStatus.ACTIVE), any(Pageable.class)
        )).thenReturn(List.of(postScrap));
        when(postHashtagRepository.findAllByPost_IdIn(List.of(4L)))
                .thenReturn(List.of());

        var result = service.findRecentInterests(1L);

        assertThat(result.infoTitles()).containsExactly("카테고리 없는 정보");
        assertThat(result.newsTitles()).containsExactly("카테고리 없는 소식");
        assertThat(result.communityTopics()).containsExactly("유형과 태그 없는 게시글");
    }

    @Test
    void ignoresScrapsWithoutTitles() {
        InfoScrap infoScrap = mock(InfoScrap.class);
        InfoItem infoItem = mock(InfoItem.class);
        when(infoScrap.getInfoItem()).thenReturn(infoItem);
        when(infoItem.getName()).thenReturn(" ");
        when(infoItem.getCategoryNames()).thenReturn(List.of("복지"));
        when(infoScrapRepository.findRecentByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(infoScrap));

        NewsScrap newsScrap = mock(NewsScrap.class);
        News news = mock(News.class);
        when(newsScrap.getNews()).thenReturn(news);
        when(news.getTitle()).thenReturn(null);
        when(news.getNewsCategory()).thenReturn(null);
        when(newsScrapRepository.findRecentVisibleByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(List.of(newsScrap));

        PostScrap postScrap = mock(PostScrap.class);
        Post post = mock(Post.class);
        when(postScrap.getPost()).thenReturn(post);
        when(post.getId()).thenReturn(5L);
        when(post.getTitle()).thenReturn("");
        when(postScrapRepository.findRecentVisibleByUserId(
                eq(1L), eq(PostStatus.ACTIVE), any(Pageable.class)
        )).thenReturn(List.of(postScrap));
        when(postHashtagRepository.findAllByPost_IdIn(List.of(5L)))
                .thenReturn(List.of());

        var result = service.findRecentInterests(1L);

        assertThat(result.infoTitles()).isEmpty();
        assertThat(result.newsTitles()).isEmpty();
        assertThat(result.communityTopics()).isEmpty();
    }
}
