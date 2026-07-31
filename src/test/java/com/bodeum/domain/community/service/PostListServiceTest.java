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
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));
        given(postAuthorRepository.findAllByIdIn(List.of(10L))).willReturn(List.of(author));
        given(pointService.getTotalPoints(Set.of(10L))).willReturn(Map.of(10L, 200));
        given(postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List.of(1L)))
                .willReturn(List.of(thumbnail));
        given(postLikeRepository.findLikedPostIds(List.of(1L), 20L)).willReturn(List.of(1L));

        var response = postListService.getPosts(20L, 0, "scrap", "  언어치료  ");

        assertThat(response.getSize()).isEqualTo(10);
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
                pageableCaptor.capture()
        );
        Sort.Order scrapOrder = pageableCaptor.getValue().getSort().getOrderFor("scrapCount");
        assertThat(scrapOrder).isNotNull();
        assertThat(scrapOrder.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void getPostsTreatsBlankKeywordAsWholePostList() {
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        var response = postListService.getPosts(10L, 0, "view", " ");

        assertThat(response).isEmpty();
        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                any(Pageable.class)
        );
        then(postAuthorRepository).shouldHaveNoInteractions();
        then(postImageRepository).shouldHaveNoInteractions();
        then(postLikeRepository).shouldHaveNoInteractions();
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void getPostsEscapesLikeWildcardCharactersInKeyword() {
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("100!%!_!!"),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        postListService.getPosts(10L, 0, "view", "100%_!");

        then(postRepository).should().findActivePosts(
                eq(PostStatus.ACTIVE),
                eq("100!%!_!!"),
                any(Pageable.class)
        );
    }

    @Test
    void getPostsMasksFullyAnonymousAuthor() {
        Post post = post(1L, 10L, PostAnonymityType.FULLY_ANONYMOUS);
        given(postRepository.findActivePosts(
                eq(PostStatus.ACTIVE),
                eq(null),
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));
        given(postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List.of(1L)))
                .willReturn(List.of());
        given(postLikeRepository.findLikedPostIds(List.of(1L), 10L)).willReturn(List.of());

        var response = postListService.getPosts(10L, 0, "comment", null);

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
    void getPostsRejectsUnsupportedSort() {
        assertThatThrownBy(() -> postListService.getPosts(10L, 0, "latest", null))
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
                any(Pageable.class)
        )).willReturn(new PageImpl<>(List.of(post), PageRequest.of(0, 10), 1));
        given(postAuthorRepository.findAllByIdIn(List.of(10L))).willReturn(List.of(author));
        given(pointService.getTotalPoints(Set.of(10L))).willReturn(Map.of());
        given(postImageRepository.findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List.of(1L)))
                .willReturn(List.of());

        var response = postListService.getPosts(null, 0, "view", null);

        assertThat(response.getContent()).singleElement().satisfies(item -> {
            assertThat(item.isLiked()).isFalse();
            assertThat(item.author().isMine()).isFalse();
        });
        then(postLikeRepository).shouldHaveNoInteractions();
    }

    private Post post(Long postId, Long userId, PostAnonymityType anonymityType) {
        Post post = Post.create(
                userId,
                PostBoardType.FREE_COMMUNICATION,
                anonymityType,
                "게시글 제목",
                "게시글 내용",
                false
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
