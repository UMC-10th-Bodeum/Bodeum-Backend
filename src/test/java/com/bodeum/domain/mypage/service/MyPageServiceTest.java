package com.bodeum.domain.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.mypage.dto.response.MyPageProfileResponse;
import com.bodeum.domain.mypage.repository.MyPageCommentRepository;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostRepository;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyPageServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private MyPageInfoScrapRepository infoScrapRepository;

    @Mock
    private MyPageNewsScrapRepository newsScrapRepository;

    @Mock
    private MyPagePostRepository postRepository;

    @Mock
    private MyPageCommentRepository commentRepository;

    @InjectMocks
    private MyPageService myPageService;

    @Test
    void getProfileReturnsProfileAndActivitySummary() {
        UserProfileResponse profile = new UserProfileResponse(
                1L,
                "민준맘",
                null,
                230,
                3,
                "꽃",
                "정성스러운 답변과 경험 공유로 커뮤니티에 "
                        + "본격적인 신뢰의 결실을 피워내는 단계입니다.",
                new UserProfileResponse.ChildProfile(
                        null,
                        null,
                        List.of()
                ),
                null,
                List.of(),
                null,
                null,
                null,
                "민준맘",
                null,
                null
        );

        given(userService.getProfile(1L))
                .willReturn(profile);

        given(infoScrapRepository.countByUserId(1L))
                .willReturn(3L);

        given(newsScrapRepository.countVisibleByUserId(1L))
                .willReturn(2L);

        given(postRepository.countVisibleByUserId(
                1L,
                PostStatus.ACTIVE
        )).willReturn(4L);

        given(commentRepository.countVisibleByUserId(
                1L,
                CommentStatus.ACTIVE,
                PostStatus.ACTIVE
        )).willReturn(7L);

        MyPageProfileResponse response =
                myPageService.getProfile(1L);

        assertThat(response.userId())
                .isEqualTo(1L);

        assertThat(response.nickname())
                .isEqualTo("민준맘");

        assertThat(response.point())
                .isEqualTo(230);

        assertThat(response.level())
                .isEqualTo(3);

        assertThat(response.badgeName())
                .isEqualTo("꽃");

        assertThat(response.activitySummary().savedInfoCount())
                .isEqualTo(5L);

        assertThat(response.activitySummary().myPostCount())
                .isEqualTo(4L);

        assertThat(response.activitySummary().myCommentCount())
                .isEqualTo(7L);
    }
}