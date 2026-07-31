package com.bodeum.domain.ai.service;

import static com.bodeum.global.common.constant.TimeConstants.SERVICE_ZONE_ID;

import com.bodeum.domain.ai.dto.response.AiChatStarterResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiChatStarterService {

    private static final String DEFAULT_DISPLAY_NAME = "보호자";
    private static final Set<String> OAUTH_DEFAULT_NICKNAMES = Arrays.stream(
                    SocialProvider.values()
            )
            .map(provider -> provider.getDisplayName() + " 사용자")
            .collect(Collectors.toUnmodifiableSet());
    private static final String GREETING_TEMPLATE = """
            안녕하세요! 저는 보듬 AI 큐레이션입니다 😊

            {{displayName}}님의 정보를 바탕으로
            복지 바우처, 재활 기관, 지원 제도 등 발달장애 아동 양육에 필요한 정보를 쉽고 빠르게 안내해드려요.

            무엇이 궁금하신가요?""";
    private static final List<String> SUGGESTED_QUESTIONS =
            Arrays.stream(AiStarterQuestionType.values())
                    .filter(AiStarterQuestionType::isSuggestedQuestion)
                    .map(AiStarterQuestionType::getContent)
                    .toList();

    private final UserService userService;
    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final Clock clock;

    @Transactional
    public AiChatStarterResponse getChatStarter(Long userId) {
        User user = userService.getCurrentUser(userId);
        String greeting = GREETING_TEMPLATE.replace(
                "{{displayName}}",
                displayName(user)
        );
        saveGreetingOnceToday(userId, greeting);
        return AiChatStarterResponse.of(greeting, SUGGESTED_QUESTIONS);
    }

    private void saveGreetingOnceToday(Long userId, String greeting) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        Instant now = Instant.now(clock);
        LocalDate today = now.atZone(SERVICE_ZONE_ID).toLocalDate();
        Instant startOfToday = today.atStartOfDay(SERVICE_ZONE_ID).toInstant();
        Instant startOfTomorrow = today.plusDays(1)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        boolean hasTodayMessages =
                aiMessageRepository
                        .existsByChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                chatRoom.getId(),
                                startOfToday,
                                startOfTomorrow
                        );
        if (hasTodayMessages) {
            return;
        }

        aiMessageRepository.save(
                AiMessage.createAiMessage(
                        chatRoom,
                        greeting,
                        false,
                        AiAnswerStatus.GREETING
                )
        );
        chatRoom.updateLastMessageAt(now);
    }

    private String displayName(User user) {
        String nickname = user.getNickname();
        if (nickname == null || nickname.isBlank()) {
            return DEFAULT_DISPLAY_NAME;
        }

        String trimmedNickname = nickname.trim();
        if (OAUTH_DEFAULT_NICKNAMES.contains(trimmedNickname)) {
            return DEFAULT_DISPLAY_NAME;
        }

        String normalizedNickname = trimmedNickname.endsWith("님")
                ? trimmedNickname.substring(0, trimmedNickname.length() - 1).trim()
                : trimmedNickname;
        return normalizedNickname.isBlank() ? DEFAULT_DISPLAY_NAME : normalizedNickname;
    }
}
