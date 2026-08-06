package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PostQueryFacadeTest {

    @Mock
    private PostViewCountService postViewCountService;

    @Mock
    private PostService postService;

    @Mock
    private PostResponse postResponse;

    @InjectMocks
    private PostQueryFacade postQueryFacade;

    @Test
    void getPostIncreasesViewCountBeforeReadingPost() {
        given(postService.getPost(10L, 1L)).willReturn(postResponse);

        postQueryFacade.getPost(10L, 1L);

        InOrder inOrder = inOrder(postViewCountService, postService);
        inOrder.verify(postViewCountService).increaseViewCount(1L);
        inOrder.verify(postService).getPost(10L, 1L);
    }

    @Test
    void getPostRejectsMissingAuthenticatedUserBeforeIncreasingViewCount() {
        assertThatThrownBy(() -> postQueryFacade.getPost(null, 1L))
                .isInstanceOf(CommunityException.class)
                .extracting(exception -> ((CommunityException) exception).getErrorCode())
                .isEqualTo(CommunityErrorCode.AUTHENTICATION_REQUIRED);

        verifyNoInteractions(postViewCountService, postService);
    }
}
