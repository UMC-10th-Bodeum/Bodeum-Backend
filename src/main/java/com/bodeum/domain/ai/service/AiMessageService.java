package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.AiTodayMessageResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bodeum.global.common.constant.TimeConstants.SERVICE_ZONE_ID;

@Service
@RequiredArgsConstructor
public class AiMessageService {

    private static final String WARNING_MESSAGE =
            "일부 사용자로부터 오류 피드백이 접수된 정보입니다.\n"
                    + "공식 기관에서 다시 확인해 주세요.";

    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;

    @Transactional(readOnly = true)
    public AiTodayMessageResponse getTodayMessages(Long userId) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        // 한국 시간 기준 오늘 0시를 Instant로 변환
        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        Instant startOfToday = today
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        // 한국 시간 기준 내일 0시를 Instant로 변환
        Instant startOfTomorrow = today
                .plusDays(1)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        List<AiMessage> messages =
                aiMessageRepository.findTodayMessages(
                        chatRoom.getId(),
                        startOfToday,
                        startOfTomorrow
                );

        if (messages.isEmpty()) {
            return AiTodayMessageResponse.of(List.of());
        }

        List<Long> messageIds = messages.stream()
                .map(AiMessage::getId)
                .toList();

        // 메시지별 출처 조회로 인한 N+1 문제를 방지하기 위해, 출처를 한 번에 조회
        List<AiResponseSourceProjection> sources =
                aiResponseSourceRepository.findAllByMessageIds(messageIds);

        // 메시지와 출처를 매핑하기 위해, 조회한 출처를 메시지 ID별로 그룹화
        Map<Long, List<AiResponseSourceProjection>> sourceMap =
                sources.stream()
                        .collect(Collectors.groupingBy(
                                AiResponseSourceProjection::getAiMessageId
                        ));

        List<AiTodayMessageResponse.Message> messageResponses =
                messages.stream()
                        .map(message -> toMessageResponse(
                                message,
                                sourceMap.getOrDefault(
                                        message.getId(),
                                        List.of()
                                )
                        ))
                        .toList();

        return AiTodayMessageResponse.of(messageResponses);
    }

    private AiTodayMessageResponse.Message toMessageResponse(
            AiMessage message,
            List<AiResponseSourceProjection> sources
    ) {
        List<AiTodayMessageResponse.Source> sourceResponses =
                sources.stream()
                        .map(source -> AiTodayMessageResponse.Source.of(
                                source.getSourceTitle(),
                                source.getSourceUrl(),
                                source.getSourceUpdatedAt()
                        ))
                        .toList();

        String warning = message.isWarning()
                ? WARNING_MESSAGE
                : null;

        return AiTodayMessageResponse.Message.of(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt(),
                sourceResponses,
                warning
        );
    }
}