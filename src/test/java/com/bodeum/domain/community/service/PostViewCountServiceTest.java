package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostViewCountServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostViewCountService postViewCountService;

    @Test
    void increaseViewCountUpdatesActivePost() {
        given(postRepository.incrementViewCount(1L, PostStatus.ACTIVE)).willReturn(1);

        postViewCountService.increaseViewCount(1L);

        then(postRepository).should().incrementViewCount(1L, PostStatus.ACTIVE);
    }

    @Test
    void increaseViewCountRejectsMissingPost() {
        given(postRepository.incrementViewCount(99L, PostStatus.ACTIVE)).willReturn(0);

        assertThatThrownBy(() -> postViewCountService.increaseViewCount(99L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.POST_NOT_FOUND);
    }
}
