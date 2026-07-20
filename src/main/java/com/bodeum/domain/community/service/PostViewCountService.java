package com.bodeum.domain.community.service;

import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostViewCountService {

    private final PostRepository postRepository;

    @Transactional
    public void increaseViewCount(Long postId) {
        if (postRepository.incrementViewCount(postId, PostStatus.ACTIVE) == 0) {
            throw new CommunityException(CommunityErrorCode.POST_NOT_FOUND);
        }
    }
}
