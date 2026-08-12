package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostImage;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.PostAuthorRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.user.entity.User;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostListServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private PostAuthorRepository postAuthorRepository;
    @Mock
    private PostImageRepository postImageRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PointService pointService;
    @InjectMocks
    private PostListService postListService;

    @Test
    void getPostsReturnsAuthorThumbnailAndViewerLikeState() {
        Post post = post(1L, 10L, PostAnonymityType.PROFILE_TAG_VISIBLE);
        PostImage thumbnail = PostImage.create(post, "https://example.com/first.jpg", 0);
        User author = user(10L, "보듬맘");

        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("언어치료"),
                eq(PostBoardType.FREE_COMMUNICATION),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 14), 1));
        given(postAuthorRepository.findAllByIdIn(List.of(10L))).willReturn(List.of(author));
        given(pointService.getTotalPoints(Set.of(10L))).willReturn(Map.of(10L, 200));
        given(postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List.of(1L)))
                .willReturn(List.of(thumbnail));
        given(postLikeRepository.findLikedPostIds(List.of(1L), 20L)).willReturn(List.of(1L));

        var response = postListService.getPosts(
                20L,
                0,
                "like",
                "  언어치료  ",
                PostBoardType.FREE_COMMUNICATION
        );

        assertThat(response.getSize()).isEqualTo(14);
        assertThat(response.getTotalElements()).isOne();
        assertThat(response.getContent()).singleElement().satisfies(item -> {
            assertThat(item.postId()).isEqualTo(1L);
            assertThat(item.thumbnailUrl()).isEqualTo("https://example.com/first.jpg");
            assertThat(item.isLiked()).isTrue();
            assertThat(item.author().authorId()).isEqualTo(10L);
            assertThat(item.author().nickname()).isEqualTo("보듬맘");
            assertThat(item.author().level()).isEqualTo(3);
            assertThat(item.author().isMine()).isFalse();
        });

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("언어치료"),
                eq(PostBoardType.FREE_COMMUNICATION),
                pageableCaptor.capture()
        );
        Sort.Order likeOrder = pageableCaptor.getValue().getSort().getOrderFor("likeCount");
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(14);
        assertThat(likeOrder).isNotNull();
        assertThat(likeOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getPostsTreatsBlankKeywordAsWholePostList() {
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 14), 0));

        var response = postListService.getPosts(10L, 0, "view", " ", null);

        assertThat(response).isEmpty();
        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                any(Pageable.class)
        );
        then(postAuthorRepository).shouldHaveNoInteractions();
        then(postImageRepository).shouldHaveNoInteractions();
        then(postLikeRepository).shouldHaveNoInteractions();
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void getPostsDefaultsToLatestSortForAuthenticatedUser() {
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 14), 0));

        postListService.getPosts(10L, 0, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort())
                .extracting(Sort.Order::getProperty)
                .containsExactly("createdAt", "id");
    }

    @Test
    void getPostsDefaultsToViewSortForAnonymousUser() {
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 14), 0));

        postListService.getPosts(null, 0, null, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getSort())
                .extracting(Sort.Order::getProperty)
                .containsExactly("viewCount", "createdAt", "id");
    }

    @Test
    void getPostsEscapesLikeWildcardCharactersInKeyword() {
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("100!%!_!!"),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 14), 0));

        postListService.getPosts(10L, 0, "view", "100%_!", null);

        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("100!%!_!!"),
                eq(null),
                any(Pageable.class)
        );
    }

    @Test
    void getSearchSuggestionsReturnsTitleAndContentPreview() {
        Post titleMatch = post(
                1L,
                10L,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "자폐스펙트럼 치료 기록",
                "치료를 시작했습니다. 꾸준히 기록하고 있습니다. 세 번째 문장입니다."
        );
        Post contentMatch = post(
                2L,
                11L,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "언어치료 후기",
                "치료를 시작했습니다. 자폐스펙트럼 아이의 경험을 공유합니다. 비슷한 고민에 도움이 되길 바랍니다. 마지막 문장입니다."
        );
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("자폐스펙트럼"),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(titleMatch, contentMatch), PageRequest.of(0, 10), 2));

        var response = postListService.getSearchSuggestions("  자폐스펙트럼  ", 10);

        assertThat(response.suggestions()).satisfiesExactly(
                suggestion -> {
                    assertThat(suggestion.title()).isEqualTo("자폐스펙트럼 치료 기록");
                    assertThat(suggestion.content()).isEqualTo(
                            "치료를 시작했습니다. 꾸준히 기록하고 있습니다. …"
                    );
                    assertThat(suggestion.type().name()).isEqualTo("POST_TITLE");
                },
                suggestion -> {
                    assertThat(suggestion.title()).isEqualTo("언어치료 후기");
                    assertThat(suggestion.content()).isEqualTo(
                            "… 자폐스펙트럼 아이의 경험을 공유합니다. 비슷한 고민에 도움이 되길 바랍니다. …"
                    );
                    assertThat(suggestion.type().name()).isEqualTo("POST_CONTENT");
                }
        );
    }

    @Test
    void getSearchSuggestionsPrioritizesContentWhenTitleAndContentMatch() {
        Post titleAndContentMatch = post(
                1L,
                10L,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "자폐스펙트럼 치료 기록",
                "치료를 시작했습니다. 자폐스펙트럼 아이의 경험을 공유합니다. 비슷한 고민에 도움이 되길 바랍니다. 마지막 문장입니다."
        );
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("자폐스펙트럼"),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(titleAndContentMatch), PageRequest.of(0, 10), 1));

        var response = postListService.getSearchSuggestions("자폐스펙트럼", 10);

        assertThat(response.suggestions()).singleElement().satisfies(suggestion -> {
            assertThat(suggestion.title()).isEqualTo("자폐스펙트럼 치료 기록");
            assertThat(suggestion.content()).isEqualTo(
                    "… 자폐스펙트럼 아이의 경험을 공유합니다. 비슷한 고민에 도움이 되길 바랍니다. …"
            );
            assertThat(suggestion.type().name()).isEqualTo("POST_CONTENT");
        });
    }

    @Test
    void getPostsMasksFullyAnonymousAuthor() {
        Post post = post(1L, 10L, PostAnonymityType.FULLY_ANONYMOUS);
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 14), 1));
        given(postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List.of(1L)))
                .willReturn(List.of());
        given(postLikeRepository.findLikedPostIds(List.of(1L), 10L)).willReturn(List.of());

        var response = postListService.getPosts(10L, 0, "comment", null, null);

        assertThat(response.getContent()).singleElement().satisfies(item -> {
            assertThat(item.author().authorId()).isNull();
            assertThat(item.author().nickname()).isEqualTo("익명");
            assertThat(item.author().profileImageUrl()).isNull();
            assertThat(item.author().level()).isNull();
            assertThat(item.author().badgeName()).isNull();
            assertThat(item.author().isMine()).isTrue();
        });
        then(postAuthorRepository).should(never()).findAllByIdIn(any());
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void getPostsRejectsRemovedScrapSort() {
        assertThatThrownBy(() -> postListService.getPosts(10L, 0, "scrap", null, null))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.INVALID_POST_LIST_SORT);

        then(postRepository).shouldHaveNoInteractions();
    }

    @Test
    void getPostsAllowsAnonymousUserWithoutPersonalizedState() {
        Post post = post(1L, 10L, PostAnonymityType.PROFILE_TAG_VISIBLE);
        User author = user(10L, "보듬맘");
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 14), 1));
        given(postAuthorRepository.findAllByIdIn(List.of(10L))).willReturn(List.of(author));
        given(pointService.getTotalPoints(Set.of(10L))).willReturn(Map.of());
        given(postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List.of(1L)))
                .willReturn(List.of());

        var response = postListService.getPosts(null, 0, "view", null, null);

        assertThat(response.getContent()).singleElement().satisfies(item -> {
            assertThat(item.isLiked()).isFalse();
            assertThat(item.author().isMine()).isFalse();
        });
        then(postLikeRepository).shouldHaveNoInteractions();
    }

    private Post post(Long postId, Long userId, PostAnonymityType anonymityType) {
        return post(postId, userId, anonymityType, "게시글 제목", "게시글 내용");
    }

    private Post post(
            Long postId,
            Long userId,
            PostAnonymityType anonymityType,
            String title,
            String content
    ) {
        Post post = Post.create(
                userId,
                PostBoardType.FREE_COMMUNICATION,
                anonymityType,
                title,
                content
        );
        ReflectionTestUtils.setField(post, "id", postId);
        ReflectionTestUtils.setField(post, "viewCount", 3);
        ReflectionTestUtils.setField(post, "likeCount", 4);
        ReflectionTestUtils.setField(post, "commentCount", 5);
        ReflectionTestUtils.setField(post, "scrapCount", 6);
        return post;
    }

    private User user(Long userId, String nickname) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                "provider-user-id",
                "user@example.com",
                nickname
        );
        ReflectionTestUtils.setField(user, "id", userId);
        user.updateProfileImage("https://example.com/profile.jpg");
        return user;
    }
}
