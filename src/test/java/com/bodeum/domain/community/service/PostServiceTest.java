package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostLike;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.HashtagRepository;
import com.bodeum.domain.community.repository.PostDisabilityTagRepository;
import com.bodeum.domain.community.repository.PostHashtagRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
import java.util.List;
import java.util.Optional;
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
    private HashtagRepository hashtagRepository;
    @Mock
    private PostHashtagRepository postHashtagRepository;
    @Mock
    private PostImageRepository postImageRepository;
    @Mock
    private PostDisabilityTagRepository postDisabilityTagRepository;
    @Mock
    private PostLikeRepository postLikeRepository;
    @Mock
    private PostScrapRepository postScrapRepository;
    @Mock
    private com.bodeum.domain.user.repository.UserRepository userRepository;
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
        then(postDisabilityTagRepository).should().saveAll(anyList());
        then(postHashtagRepository).should().saveAll(anyList());
        then(postImageRepository).should().saveAll(anyList());
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
                        null,
                        PostAnonymityType.FULLY_ANONYMOUS,
                        "수정 제목",
                        null,
                        null,
                        null,
                        null,
                        false
                )
        );

        assertThat(response.title()).isEqualTo("수정 제목");
        assertThat(response.content()).isEqualTo("게시글 내용");
        assertThat(response.anonymityType()).isEqualTo(PostAnonymityType.FULLY_ANONYMOUS);
        assertThat(response.authorId()).isNull();
        assertThat(response.isMine()).isTrue();
    }

    @Test
    void updatePostRejectsNonOwner() {
        given(postRepository.findByIdAndStatusForUpdate(1L, PostStatus.ACTIVE))
                .willReturn(Optional.of(post(1L, 10L)));

        assertThatThrownBy(() -> postService.updatePost(
                20L,
                1L,
                new UpdatePostRequest(null, null, "수정 제목", null, null, null, null, null)
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
                "익명 게시글 내용",
                false
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
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                List.of(DisabilityType.AUTISM),
                List.of("육아"),
                List.of("https://example.com/image.jpg"),
                true
        );
    }

    private Post post(Long postId, Long userId) {
        Post post = Post.create(
                userId,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                false
        );
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }
}
