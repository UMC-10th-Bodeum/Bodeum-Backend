package com.bodeum.domain.ai.service;

import static com.bodeum.global.common.constant.TimeConstants.SERVICE_ZONE_ID;

import com.bodeum.domain.ai.dto.response.AiMessageResponse;
import com.bodeum.domain.ai.dto.response.AiMessageHistoryResponse;
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
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMessageQueryService {

    private static final int HISTORY_PAGE_SIZE = 20;
    private static final int HISTORY_LOOKBACK_DAYS = 7;

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

        Map<Long, List<AiResponseSourceProjection>> sourceMap = loadSourceMap(messages);
        List<AiMessageResponse> messageResponses = messages.stream()
                .map(message -> toMessageResponse(
                        message,
                        sourceMap.getOrDefault(message.getId(), List.of())))
                .toList();
        return AiTodayMessageResponse.of(messageResponses);
    }

    @Transactional(readOnly = true)
    public AiMessageHistoryResponse getHistoryMessages(
            Long userId,
            Long cursor
    ) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        Instant startOfToday = today.atStartOfDay(SERVICE_ZONE_ID).toInstant();
        Instant historyStart = today.minusDays(HISTORY_LOOKBACK_DAYS)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        List<AiMessage> fetchedMessages = aiMessageRepository.findHistoryMessages(
                chatRoom.getId(),
                historyStart,
                startOfToday,
                cursor,
                PageRequest.of(0, HISTORY_PAGE_SIZE + 1)
        );

        if (fetchedMessages.isEmpty()) {
            return AiMessageHistoryResponse.of(List.of(), null, false);
        }

        boolean hasNext = fetchedMessages.size() > HISTORY_PAGE_SIZE;
        List<AiMessage> pageMessages = hasNext
                ? fetchedMessages.subList(0, HISTORY_PAGE_SIZE)
                : fetchedMessages;

        Map<Long, List<AiResponseSourceProjection>> sourceMap = loadSourceMap(pageMessages);
        LinkedHashMap<LocalDate, List<AiMessageResponse>> groupedMessages = new LinkedHashMap<>();

        // 서비스 시간대를 기준으로 메시지를 날짜별로 그룹화
        for (AiMessage message : pageMessages) {
            LocalDate messageDate = message.getCreatedAt()
                    .atZone(SERVICE_ZONE_ID)
                    .toLocalDate();
            groupedMessages.computeIfAbsent(messageDate, ignored -> new ArrayList<>())
                    .add(toMessageResponse(
                            message,
                            sourceMap.getOrDefault(message.getId(), List.of())));
        }

        // 조회된 최신순 메시지를 같은 날짜 내 오래된 순으로 변환
        List<AiMessageHistoryResponse.HistoryDateGroup> dateGroups = groupedMessages.entrySet()
                .stream()
                .map(entry -> AiMessageHistoryResponse.HistoryDateGroup.of(
                        entry.getKey(),
                        reverseCopy(entry.getValue())))
                .toList();

        Long nextCursor = pageMessages.getLast().getId();
        return AiMessageHistoryResponse.of(dateGroups, nextCursor, hasNext);
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

    // N+1 방지를 위해 메시지별 출처를 한 번에 조회한 후, 메시지 ID별로 그룹화
    private Map<Long, List<AiResponseSourceProjection>> loadSourceMap(
            List<AiMessage> messages
    ) {
        List<Long> messageIds = messages.stream()
                .map(AiMessage::getId)
                .toList();
        return aiResponseSourceRepository.findAllByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(AiResponseSourceProjection::getAiMessageId));
    }

    private List<AiMessageResponse> reverseCopy(
            List<AiMessageResponse> messages
    ) {
        List<AiMessageResponse> reversedMessages = new ArrayList<>(messages);
        Collections.reverse(reversedMessages);
        return List.copyOf(reversedMessages);
    }
}
