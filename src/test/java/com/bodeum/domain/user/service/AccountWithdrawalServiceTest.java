package com.bodeum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.bodeum.domain.ai.service.AiWithdrawalService;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.community.service.CommentService;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.domain.info.service.InfoScrapService;
import com.bodeum.domain.news.service.NewsScrapService;
import com.bodeum.domain.search.service.SearchService;
import com.bodeum.domain.user.dto.response.UserWithdrawResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccountWithdrawalServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;
    @Mock
    private SearchService searchService;
    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;
    @Mock
    private InfoScrapService infoScrapService;
    @Mock
    private NewsScrapService newsScrapService;
    @Mock
    private AiWithdrawalService aiWithdrawalService;
    @Mock
    private S3ImageStorage s3ImageStorage;

    @InjectMocks
    private AccountWithdrawalService accountWithdrawalService;

    private User activeUser(String profileImageUrl) {
        User user = User.createSocialUser(SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        ReflectionTestUtils.setField(user, "id", 1L);
        if (profileImageUrl != null) {
            ReflectionTestUtils.setField(user, "profileImageUrl", profileImageUrl);
        }
        return user;
    }

    @Test
    void withdrawOrchestratesAllDomainCleanupsAndDeletesProfileImage() {
        User user = activeUser("https://cdn.example.com/profile-images/abc.jpg");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userService.withdraw(1L)).willReturn(UserWithdrawResponse.ofSuccess());

        UserWithdrawResponse response = accountWithdrawalService.withdraw(1L);

        assertThatResponseSucceeds(response);
        // 검색기록·스크랩·좋아요 삭제가 각 도메인 핸들러로 위임된다.
        then(searchService).should().deleteUserSearchLogs(1L);
        then(postService).should().deleteUserScrapsAndLikes(1L);
        then(commentService).should().deleteUserCommentLikes(1L);
        then(infoScrapService).should().deleteUserScraps(1L);
        then(newsScrapService).should().deleteUserScraps(1L);
        then(aiWithdrawalService).should().deleteUserAiData(1L);
        // user/auth 개인정보 파기는 UserService에 위임된다.
        then(userService).should().withdraw(1L);
        // 프로필 이미지는 캡처된 원본 URL로 S3에서 삭제된다(트랜잭션 비활성 상태이므로 즉시 호출).
        then(s3ImageStorage).should().delete("https://cdn.example.com/profile-images/abc.jpg");
    }

    @Test
    void withdrawSkipsS3DeleteWhenNoProfileImage() {
        User user = activeUser(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(userService.withdraw(1L)).willReturn(UserWithdrawResponse.ofSuccess());

        accountWithdrawalService.withdraw(1L);

        then(s3ImageStorage).should(never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void withdrawRejectsAlreadyWithdrawnUserWithoutTouchingOtherDomains() {
        User user = activeUser(null);
        user.withdraw();
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        assertThatThrownBy(() -> accountWithdrawalService.withdraw(1L))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AuthErrorCode.ALREADY_WITHDRAWN);

        then(searchService).shouldHaveNoInteractions();
        then(postService).shouldHaveNoInteractions();
        then(commentService).shouldHaveNoInteractions();
        then(infoScrapService).shouldHaveNoInteractions();
        then(newsScrapService).shouldHaveNoInteractions();
        then(aiWithdrawalService).shouldHaveNoInteractions();
        then(userService).shouldHaveNoInteractions();
        then(s3ImageStorage).shouldHaveNoInteractions();
    }

    private void assertThatResponseSucceeds(UserWithdrawResponse response) {
        org.assertj.core.api.Assertions.assertThat(response.success()).isTrue();
    }
}
