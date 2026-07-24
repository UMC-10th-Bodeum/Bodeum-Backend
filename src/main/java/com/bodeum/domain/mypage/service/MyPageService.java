package com.bodeum.domain.mypage.service;

import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.mypage.dto.response.MyPageProfileResponse;
import com.bodeum.domain.mypage.dto.response.MyPageProfileResponse.ActivitySummary;
import com.bodeum.domain.mypage.repository.MyPageCommentRepository;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostRepository;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserService userService;
    private final MyPageInfoScrapRepository infoScrapRepository;
    private final MyPageNewsScrapRepository newsScrapRepository;
    private final MyPagePostRepository postRepository;
    private final MyPageCommentRepository commentRepository;

    @Transactional(readOnly = true)
    public MyPageProfileResponse getProfile(Long userId) {
        UserProfileResponse profile = userService.getProfile(userId);

        long savedInfoCount =
                infoScrapRepository.countByUserId(userId)
                        + newsScrapRepository.countVisibleByUserId(userId);

        long myPostCount = postRepository.countVisibleByUserId(
                userId,
                PostStatus.ACTIVE
        );

        long myCommentCount = commentRepository.countVisibleByUserId(
                userId,
                CommentStatus.ACTIVE,
                PostStatus.ACTIVE
        );

        ActivitySummary activitySummary = new ActivitySummary(
                savedInfoCount,
                myPostCount,
                myCommentCount
        );

        return MyPageProfileResponse.of(
                profile,
                activitySummary
        );
    }
}