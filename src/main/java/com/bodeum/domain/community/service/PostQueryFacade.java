package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostQueryFacade {

    private final PostViewCountService postViewCountService;
    private final PostService postService;

    public PostResponse getPost(Long userId, Long postId) {
        validateAuthenticatedUser(userId);
        postViewCountService.increaseViewCount(postId);
        return postService.getPost(userId, postId);
    }

    private void validateAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new CommunityException(CommunityErrorCode.AUTHENTICATION_REQUIRED);
        }
    }
}
