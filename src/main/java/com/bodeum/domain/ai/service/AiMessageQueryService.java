package com.bodeum.domain.ai.service;

import static com.bodeum.global.common.constant.TimeConstants.SERVICE_ZONE_ID;

import com.bodeum.domain.ai.dto.response.AiMessageResponse;
import com.bodeum.domain.ai.dto.response.AiMessageSourceResponse;
import com.bodeum.domain.ai.dto.response.AiMessageWarningResponse;
import com.bodeum.domain.ai.dto.response.AiTodayMessageResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMessageQueryService {

    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;

    @Transactional(readOnly = true)
    public AiTodayMessageResponse getTodayMessages(Long userId) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        Instant startOfToday = today.atStartOfDay(SERVICE_ZONE_ID).toInstant();
        Instant startOfTomorrow = today.plusDays(1)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        List<AiMessage> messages = aiMessageRepository.findTodayMessages(
                chatRoom.getId(), startOfToday, startOfTomorrow);
        if (messages.isEmpty()) {
            return AiTodayMessageResponse.of(List.of());
        }

        List<Long> messageIds = messages.stream()
                .map(AiMessage::getId)
                .toList();
        Map<Long, List<AiResponseSourceProjection>> sourceMap =
                aiResponseSourceRepository.findAllByMessageIds(messageIds).stream()
                        .collect(Collectors.groupingBy(
                                AiResponseSourceProjection::getAiMessageId));

        List<AiMessageResponse> messageResponses = messages.stream()
                .map(message -> toMessageResponse(
                        message,
                        sourceMap.getOrDefault(message.getId(), List.of())))
                .toList();
        return AiTodayMessageResponse.of(messageResponses);
    }

    private AiMessageResponse toMessageResponse(
            AiMessage message,
            List<AiResponseSourceProjection> sources
    ) {
        if (message.getSenderType() == SenderType.USER) {
            return AiMessageResponse.user(
                    message.getId(), message.getContent(), message.getCreatedAt());
        }

        List<AiMessageSourceResponse> sourceResponses = sources.stream()
                .map(source -> new AiMessageSourceResponse(
                        source.getSourceType(),
                        source.getSourceId(),
                        source.getSourceTitle(),
                        source.getSourceUrl(),
                        source.getSourceUpdatedAt()))
                .toList();

        AiAnswerStatus answerStatus = message.getAiAnswerStatus();
        if (answerStatus == null) {
            answerStatus = sourceResponses.isEmpty()
                    ? AiAnswerStatus.NO_EVIDENCE
                    : AiAnswerStatus.ANSWERED;
        }

        if (answerStatus == AiAnswerStatus.NO_EVIDENCE) {
            return AiMessageResponse.noEvidence(
                    message.getId(),
                    message.getSenderType(),
                    message.getContent(),
                    message.getCreatedAt());
        }

        return AiMessageResponse.sourceBacked(
                message.getId(),
                message.getSenderType(),
                answerStatus,
                message.getContent(),
                message.getCreatedAt(),
                sourceResponses,
                message.isWarning() ? AiMessageWarningResponse.incorrectSource() : null);
    }
}
