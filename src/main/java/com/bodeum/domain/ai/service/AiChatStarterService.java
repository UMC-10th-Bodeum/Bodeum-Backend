package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.AiChatStarterResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiChatStarterService {

    private static final String DEFAULT_DISPLAY_NAME = "보호자";
    private static final String GREETING_TEMPLATE = """
            안녕하세요! 저는 보듬 AI 큐레이션입니다 😊

            {{displayName}}님의 정보를 바탕으로
            복지 바우처, 재활 기관, 지원 제도 등 발달장애 아동 양육에 필요한 정보를 쉽고 빠르게 안내해드려요.

            무엇이 궁금하신가요?""";
    private static final List<String> SUGGESTED_QUESTIONS = List.of(
            "참고하면 좋을 복지사이트 알려줘",
            "우리 동네 재활센터 추천해줘",
            "장애아동 의료비 지원이 궁금해",
            "장애 진단 후 첫 번째로 해야 할 일",
            "바우처 신청 방법 알려줘"
    );

    private final UserService userService;

    @Transactional(readOnly = true)
    public AiChatStarterResponse getChatStarter(Long userId) {
        User user = userService.getCurrentUser(userId);
        String greeting = GREETING_TEMPLATE.replace(
                "{{displayName}}",
                displayName(user)
        );
        return AiChatStarterResponse.of(greeting, SUGGESTED_QUESTIONS);
    }

    private String displayName(User user) {
        String nickname = user.getNickname();
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_DISPLAY_NAME;
        }

        String trimmedNickname = nickname.trim();
        if (user.getProvider() != null
                && trimmedNickname.equals(user.getProvider().getDisplayName() + " 사용자")) {
            return DEFAULT_DISPLAY_NAME;
        }

        return trimmedNickname;
    }
}
