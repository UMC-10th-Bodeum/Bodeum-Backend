package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.response.PostResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostQueryFacade {

    private final PostViewCountService postViewCountService;
    private final PostService postService;

    public PostResponse getPost(Long userId, Long postId) {
        postViewCountService.increaseViewCount(postId);
        return postService.getPost(userId, postId);
    }
}
