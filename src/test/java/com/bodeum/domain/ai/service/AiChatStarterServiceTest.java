package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.ai.dto.response.AiChatStarterResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiChatStarterServiceTest {

    private UserService userService;
    private AiChatStarterService aiChatStarterService;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        aiChatStarterService = new AiChatStarterService(userService);
    }

    @Test
    void returnsPersonalizedGreetingAndFiveSuggestedQuestions() {
        User user = mock(User.class);
        when(userService.getCurrentUser(10L)).thenReturn(user);
        when(user.getNickname()).thenReturn("민정맘");

        AiChatStarterResponse response = aiChatStarterService.getChatStarter(10L);

        assertThat(response.greeting())
                .contains(
                        "안녕하세요! 저는 보듬 AI 큐레이션입니다 😊",
                        "민정맘님의 정보를 바탕으로",
                        "복지 바우처, 재활 기관, 지원 제도",
                        "무엇이 궁금하신가요?"
                );
        assertThat(response.suggestedQuestions())
                .containsExactly(
                        "참고하면 좋을 복지사이트 알려줘",
                        "우리 동네 재활센터 추천해줘",
                        "장애아동 의료비 지원이 궁금해",
                        "장애 진단 후 첫 번째로 해야 할 일",
                        "바우처 신청 방법 알려줘"
                );
    }

    @Test
    void usesGuardianFallbackWhenNicknameIsBlank() {
        User user = mock(User.class);
        when(userService.getCurrentUser(10L)).thenReturn(user);
        when(user.getNickname()).thenReturn(" ");

        AiChatStarterResponse response = aiChatStarterService.getChatStarter(10L);

        assertThat(response.greeting())
                .contains("보호자님의 정보를 바탕으로");
    }

    @Test
    void doesNotDuplicateHonorificWhenNicknameEndsWithHonorific() {
        User user = mock(User.class);
        when(userService.getCurrentUser(10L)).thenReturn(user);
        when(user.getNickname()).thenReturn("민정님");

        AiChatStarterResponse response = aiChatStarterService.getChatStarter(10L);

        assertThat(response.greeting())
                .contains("민정님의 정보를 바탕으로")
                .doesNotContain("민정님님의 정보를 바탕으로");
    }

    @Test
    void usesGuardianFallbackForOauthGeneratedNicknameWithoutProvider() {
        User user = mock(User.class);
        when(userService.getCurrentUser(10L)).thenReturn(user);
        when(user.getNickname()).thenReturn(SocialProvider.KAKAO.getDisplayName() + " 사용자");

        AiChatStarterResponse response = aiChatStarterService.getChatStarter(10L);

        assertThat(response.greeting())
                .contains("보호자님의 정보를 바탕으로")
                .doesNotContain(SocialProvider.KAKAO.getDisplayName() + " 사용자님");
    }
}
