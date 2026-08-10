package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostImage;
import com.bodeum.domain.community.entity.PostLike;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.CommentRepository;
import com.bodeum.domain.community.repository.PostAuthorRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PostImageRepository postImageRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostScrapRepository postScrapRepository;
    @Mock
    private PostAuthorRepository postAuthorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PointService pointService;
    @InjectMocks
    private PostService postService;

    @Test
    void createPostStoresPostAndRelatedData() {
        given(postRepository.save(any(Post.class))).willAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            ReflectionTestUtils.setField(post, "id", 1L);
            return post;
        });

        PostResponse response = postService.createPost(10L, createRequest());

        assertThat(response.postId()).isEqualTo(1L);
        assertThat(response.authorId()).isEqualTo(10L);
        assertThat(response.isQuestion()).isTrue();
        assertThat(response.title()).isEqualTo("게시글 제목");
        then(postImageRepository).should().saveAll(anyList());
        then(pointService).should().grantActivityPoint(
                10L,
                PointEventType.COMMUNITY_POST_CREATED,
                1L,
                10L
        );
    }

    @Test
    void updatePostChangesOnlyRequestedFieldsForOwner() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        PostResponse response = postService.updatePost(
                10L,
                1L,
                new UpdatePostRequest(
                        PostBoardType.INFORMATION_QUESTION,
                        PostAnonymityType.FULLY_ANONYMOUS,
                        "수정 제목",
                        null,
                        null
                )
        );

        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.content()).isEqualTo("게시글 내용");
        assertThat(response.anonymityType()).isEqualTo(PostAnonymityType.FULLY_ANONYMOUS);
        assertThat(response.authorId()).isNull();
        assertThat(response.isMine()).isTrue();
        assertThat(response.isQuestion()).isTrue();
        then(postImageRepository).should(never()).deleteAllByPost_Id(any());
        then(postImageRepository).should(never()).saveAll(anyList());
    }

    @Test
    void updatePostReplacesImagesWithRequestedFinalList() {
        Post post = post(1L, 10L);
        List<String> finalImageUrls = List.of(
                "https://example.com/existing-1.jpg",
                "https://example.com/existing-2.jpg",
                "https://example.com/new-1.jpg"
        );
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        postService.updatePost(
                10L,
                1L,
                new UpdatePostRequest(null, null, null, null, finalImageUrls)
        );

        then(postImageRepository).should().deleteAllByPost_Id(1L);
        then(postImageRepository).should().flush();
        then(postImageRepository).should().saveAll(argThat(images -> {
            List<PostImage> savedImages = StreamSupport.stream(images.spliterator(), false).toList();
            return savedImages.stream().map(PostImage::getImageUrl).toList().equals(finalImageUrls)
                    && savedImages.stream().map(PostImage::getSortOrder).toList().equals(List.of(0, 1, 2));
        }));
    }

    @Test
    void updatePostRejectsNonOwner() {
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post(1L, 10L)));

        assertThatThrownBy(() -> postService.updatePost(
                20L,
                1L,
                new UpdatePostRequest(null, null, "수정 제목", null, null)
        ))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.POST_FORBIDDEN);
    }

    @Test
    void deletePostMarksOwnedPostAsDeleted() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        postService.deletePost(10L, 1L);

        assertThat(post.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(post.getDeletedAt()).isNotNull();
    }

    @Test
    void getPostRejectsMissingPost() {
        assertThatThrownBy(() -> postService.getPost(10L, 99L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
    }

    @Test
    void getAnonymousPostHidesAuthorIdAndCalculatesOwnership() {
        Post post = Post.create(
                10L,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.FULLY_ANONYMOUS,
                "익명 게시글",
                "익명 게시글 내용"
        );
        ReflectionTestUtils.setField(post, "id", 1L);
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postLikeRepository.existsByPost_IdAndUserId(1L, 20L)).willReturn(true);
        given(postScrapRepository.existsByPost_IdAndUserId(1L, 20L)).willReturn(true);

        PostResponse response = postService.getPost(20L, 1L);

        assertThat(response.authorId()).isNull();
        assertThat(response.isMine()).isFalse();
        assertThat(response.isLiked()).isTrue();
        assertThat(response.isScrapped()).isTrue();
    }

    @Test
    void getPostAllowsAnonymousViewerWithoutPersonalizedState() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        PostResponse response = postService.getPost(null, 1L);

        assertThat(response.isMine()).isFalse();
        assertThat(response.isLiked()).isFalse();
        assertThat(response.isScrapped()).isFalse();
        then(postLikeRepository).shouldHaveNoInteractions();
        then(postScrapRepository).shouldHaveNoInteractions();
    }

    @Test
    void getPostProvidesVisibleAuthorLevelAndChildAge() {
        Post post = post(1L, 10L);
        User author = User.createSocialUser(
                SocialProvider.KAKAO,
                "provider-user-id",
                "user@example.com",
                "보듬맘"
        );
        ReflectionTestUtils.setField(author, "id", 10L);
        author.updateChildProfile(
                "아이",
                YearMonth.now().minusYears(7).toString(),
                List.of(),
                null
        );
        given(postRepository.findByIdAndStatusAndDeletedAtIsNull(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postAuthorRepository.findById(10L)).willReturn(Optional.of(author));
        given(pointService.getTotalPoint(10L)).willReturn(200);

        PostResponse response = postService.getPost(null, 1L);

        assertThat(response.authorLevel()).isEqualTo(3);
        assertThat(response.childAge()).isEqualTo(7);
    }

    @Test
    void createPostRejectsMissingAuthenticatedUser() {
        assertThatThrownBy(() -> postService.createPost(null, createRequest()))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.AUTHENTICATION_REQUIRED);
    }

    @Test
    void likePostCreatesLikeAndIncreasesCount() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postLikeRepository.existsByPost_IdAndUserId(1L, 20L)).willReturn(false);

        var response = postService.likePost(20L, 1L);

        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isOne();
        assertThat(post.getLikeCount()).isOne();
        then(postLikeRepository).should().save(any(PostLike.class));
        then(pointService).should().grantActivityPoint(
                10L,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                1L,
                20L
        );
    }

    @Test
    void likePostDoesNotIncreaseCountWhenLikeAlreadyExists() {
        Post post = post(1L, 10L);
        post.increaseLikeCount();
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postLikeRepository.existsByPost_IdAndUserId(1L, 20L)).willReturn(true);

        var response = postService.likePost(20L, 1L);

        assertThat(response.likeCount()).isOne();
        then(postLikeRepository).should(never()).save(any(PostLike.class));
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void unlikePostDeletesLikeAndDecreasesCount() {
        Post post = post(1L, 10L);
        post.increaseLikeCount();
        PostLike postLike = PostLike.create(post, 20L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postLikeRepository.findByPost_IdAndUserId(1L, 20L)).willReturn(Optional.of(postLike));

        var response = postService.unlikePost(20L, 1L);

        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isZero();
        then(postLikeRepository).should().delete(postLike);
        then(pointService).should().revokeActivityPoint(
                10L,
                PointEventType.COMMUNITY_POST_LIKE_RECEIVED,
                1L,
                20L
        );
    }

    @Test
    void likeOwnPostDoesNotGrantPoint() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));

        postService.likePost(10L, 1L);

        assertThat(post.getLikeCount()).isOne();
        then(pointService).shouldHaveNoInteractions();
    }

    @Test
    void scrapPostCreatesScrapAndIncreasesCount() {
        Post post = post(1L, 10L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postScrapRepository.existsByPost_IdAndUserId(1L, 20L)).willReturn(false);

        var response = postService.scrapPost(20L, 1L);

        assertThat(response.isScrapped()).isTrue();
        assertThat(response.scrapCount()).isOne();
        then(postScrapRepository).should().save(any(PostScrap.class));
    }

    @Test
    void unscrapPostDeletesScrapAndDecreasesCount() {
        Post post = post(1L, 10L);
        post.increaseScrapCount();
        PostScrap postScrap = PostScrap.create(post, 20L);
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post));
        given(postScrapRepository.findByPost_IdAndUserId(1L, 20L)).willReturn(Optional.of(postScrap));

        var response = postService.unscrapPost(20L, 1L);

        assertThat(response.isScrapped()).isFalse();
        assertThat(response.scrapCount()).isZero();
        then(postScrapRepository).should().delete(postScrap);
    }

    private CreatePostRequest createRequest() {
        return new CreatePostRequest(
                PostBoardType.INFORMATION_QUESTION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                List.of("https://example.com/image.jpg")
        );
    }

    private Post post(Long postId, Long userId) {
        Post post = Post.create(
                userId,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용"
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
