package com.bodeum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.auth.enumtype.SocialProvider;
import com.bodeum.domain.user.dto.response.UserHeaderResponse;
import com.bodeum.domain.user.entity.UserAccount;
import com.bodeum.domain.user.repository.UserAccountRepository;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserService userService;

    @Test
    void headerInfoReturnsLoggedOutWhenAuthenticationIsNull() {
        // 헤더/사이드바 조회는 비로그인(인증 없음)이어도 예외 없이 비로그인 응답을 반환한다.
        UserHeaderResponse response = userService.getHeaderInfo(null);

        assertThat(response.isLoggedIn()).isFalse();
    }

    @Test
    void headerInfoReturnsLoggedOutWhenPrincipalIsNotAuthUser() {
        given(authentication.getPrincipal()).willReturn("anonymousUser");

        UserHeaderResponse response = userService.getHeaderInfo(authentication);

        assertThat(response.isLoggedIn()).isFalse();
    }

    @Test
    void headerInfoReturnsUserInfoWhenLoggedIn() {
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        given(authentication.getPrincipal())
                .willReturn(new AuthUserPrincipal(1L, SocialProvider.KAKAO, "민준맘", "parent@example.com"));
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserHeaderResponse response = userService.getHeaderInfo(authentication);

        assertThat(response.isLoggedIn()).isTrue();
        assertThat(response.nickname()).isEqualTo("민준맘");
        assertThat(response.level()).isEqualTo(1);
        assertThat(response.badgeName()).isEqualTo("새싹");
    }

    @Test
    void headerInfoFallsBackToLoggedOutWhenUserWithdrawn() {
        UserAccount userAccount = UserAccount.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "민준맘");
        userAccount.withdraw();
        given(authentication.getPrincipal())
                .willReturn(new AuthUserPrincipal(1L, SocialProvider.KAKAO, "민준맘", "parent@example.com"));
        given(userAccountRepository.findById(1L)).willReturn(Optional.of(userAccount));

        UserHeaderResponse response = userService.getHeaderInfo(authentication);

        assertThat(response.isLoggedIn()).isFalse();
    }
}
