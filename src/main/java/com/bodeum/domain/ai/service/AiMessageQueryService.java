package com.bodeum.domain.ai.service;

import static com.bodeum.global.common.constant.TimeConstants.SERVICE_ZONE_ID;

import com.bodeum.domain.ai.dto.response.AiMessageHistoryResponse;
import com.bodeum.domain.ai.dto.response.AiMessageFeedbackResponse;
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
import com.bodeum.domain.ai.repository.AiFeedbackRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import com.bodeum.domain.ai.repository.projection.AiFeedbackProjection;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiMessageQueryService {

    private static final int TODAY_MESSAGE_LIMIT = 20;
    private static final int HISTORY_MESSAGE_LIMIT = 20;
    private static final int HISTORY_LOOKBACK_DAYS = 7;

    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiFeedbackRepository aiFeedbackRepository;

    @Transactional(readOnly = true)
    public AiTodayMessageResponse getTodayMessages(
            Long userId,
            Long cursorId,
            Instant cursorCreatedAt
    ) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        LocalDate today = LocalDate.now(SERVICE_ZONE_ID);
        Instant startOfToday = today.atStartOfDay(SERVICE_ZONE_ID).toInstant();
        Instant startOfTomorrow = today.plusDays(1)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        List<AiMessage> fetchedMessages = aiMessageRepository.findTodayMessages(
                chatRoom.getId(),
                startOfToday,
                startOfTomorrow,
                cursorId,
                cursorCreatedAt,
                PageRequest.of(0, TODAY_MESSAGE_LIMIT + 1)
        );
        if (fetchedMessages.isEmpty()) {
            return AiTodayMessageResponse.of(List.of(), null, false);
        }

        boolean hasNext = fetchedMessages.size() > TODAY_MESSAGE_LIMIT;
        List<AiMessage> pageMessages = List.copyOf(
                fetchedMessages.subList(
                        0,
                        Math.min(fetchedMessages.size(), TODAY_MESSAGE_LIMIT)
                )
        );

        Map<Long, List<AiResponseSourceProjection>> sourceMap = loadSourceMap(pageMessages);
        Map<Long, AiMessageFeedbackResponse> feedbackMap = loadFeedbackMap(pageMessages);
        List<AiMessageResponse> messageResponses = pageMessages.stream()
                .map(message -> toMessageResponse(
                        message,
                        sourceMap.getOrDefault(message.getId(), List.of()),
                        feedbackMap.get(message.getId())))
                .toList();

        AiMessage oldestMessage = pageMessages.getLast();
        return AiTodayMessageResponse.of(
                reverseCopy(messageResponses),
                new AiTodayMessageResponse.Cursor(
                        oldestMessage.getId(),
                        oldestMessage.getCreatedAt()
                ),
                hasNext
        );
    }

    @Transactional(readOnly = true)
    public AiMessageHistoryResponse getHistoryMessages(
            Long userId,
            Long cursorId,
            Instant cursorCreatedAt
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
                cursorId,
                cursorCreatedAt
        );

        if (fetchedMessages.isEmpty()) {
            return AiMessageHistoryResponse.of(List.of(), null, false);
        }

        List<AiMessage> pageMessages = selectPageMessages(fetchedMessages);
        boolean hasNext = pageMessages.size() < fetchedMessages.size();

        Map<Long, List<AiResponseSourceProjection>> sourceMap = loadSourceMap(pageMessages);
        Map<Long, AiMessageFeedbackResponse> feedbackMap = loadFeedbackMap(pageMessages);
        LinkedHashMap<LocalDate, List<AiMessageResponse>> groupedMessages = new LinkedHashMap<>();

        for (AiMessage message : pageMessages) {
            LocalDate messageDate = message.getCreatedAt()
                    .atZone(SERVICE_ZONE_ID)
                    .toLocalDate();
            groupedMessages.computeIfAbsent(messageDate, ignored -> new ArrayList<>())
                    .add(toMessageResponse(
                            message,
                            sourceMap.getOrDefault(message.getId(), List.of()),
                            feedbackMap.get(message.getId())));
        }

        List<AiMessageHistoryResponse.HistoryDateGroup> dateGroupsDescending =
                groupedMessages.entrySet()
                .stream()
                .map(entry -> AiMessageHistoryResponse.HistoryDateGroup.of(
                        entry.getKey(),
                        reverseCopy(entry.getValue())))
                .toList();
        List<AiMessageHistoryResponse.HistoryDateGroup> dateGroups =
                reverseCopy(dateGroupsDescending);

        Long nextCursor = pageMessages.getLast().getId();
        Instant nextCursorCreatedAt = pageMessages.getLast().getCreatedAt();
        return AiMessageHistoryResponse.of(
                dateGroups,
                new AiMessageHistoryResponse.Cursor(nextCursor, nextCursorCreatedAt),
                hasNext
        );
    }

    private AiMessageResponse toMessageResponse(
            AiMessage message,
            List<AiResponseSourceProjection> sources,
            AiMessageFeedbackResponse feedback
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
                    message.getCreatedAt())
                    .withFeedback(feedback);
        }

        if (answerStatus == AiAnswerStatus.REGION_REQUIRED) {
            return AiMessageResponse.regionRequired(
                    message.getId(),
                    message.getSenderType(),
                    message.getContent(),
                    message.getCreatedAt())
                    .withFeedback(feedback);
        }

        if (answerStatus == AiAnswerStatus.CLARIFICATION_REQUIRED) {
            return AiMessageResponse.clarificationRequired(
                    message.getId(),
                    message.getSenderType(),
                    message.getContent(),
                    message.getCreatedAt())
                    .withFeedback(feedback);
        }

        if (answerStatus == AiAnswerStatus.GREETING) {
            return AiMessageResponse.greeting(
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
                message.isWarning() ? AiMessageWarningResponse.incorrectSource() : null)
                .withFeedback(feedback);
    }

    // N+1 방지를 위해 메시지별 출처를 한 번에 조회한 후 메시지 ID별로 그룹화
    private Map<Long, List<AiResponseSourceProjection>> loadSourceMap(
            List<AiMessage> messages
    ) {
        List<Long> messageIds = messages.stream()
                .map(AiMessage::getId)
                .toList();
        return aiResponseSourceRepository.findAllByMessageIds(messageIds).stream()
                .collect(Collectors.groupingBy(AiResponseSourceProjection::getAiMessageId));
    }

    private Map<Long, AiMessageFeedbackResponse> loadFeedbackMap(
            List<AiMessage> messages
    ) {
        List<Long> messageIds = messages.stream()
                .filter(message -> message.getSenderType() == SenderType.AI)
                .map(AiMessage::getId)
                .toList();
        if (messageIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<AiFeedbackProjection>> projectionsByMessageId =
                aiFeedbackRepository.findAllWithReasonsByMessageIds(messageIds).stream()
                        .collect(Collectors.groupingBy(
                                AiFeedbackProjection::getAiMessageId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        Map<Long, AiMessageFeedbackResponse> feedbackMap = new LinkedHashMap<>();
        projectionsByMessageId.forEach((messageId, projections) -> {
            AiFeedbackProjection feedback = projections.getFirst();
            feedbackMap.put(messageId, new AiMessageFeedbackResponse(
                    feedback.getAiFeedbackId(),
                    feedback.getFeedbackType(),
                    projections.stream()
                            .map(AiFeedbackProjection::getReason)
                            .filter(java.util.Objects::nonNull)
                            .toList()
            ));
        });
        return Map.copyOf(feedbackMap);
    }

    // 같은 날짜 그룹이 페이지 사이에서 분리되지 않도록 최소 20개 이후 날짜 경계에서 자른다.
    private List<AiMessage> selectPageMessages(
            List<AiMessage> fetchedMessages
    ) {
        List<AiMessage> selectedMessages = new ArrayList<>();
        LocalDate currentDate = null;

        for (AiMessage message : fetchedMessages) {
            LocalDate messageDate = message.getCreatedAt()
                    .atZone(SERVICE_ZONE_ID)
                    .toLocalDate();

            if (selectedMessages.size() >= HISTORY_MESSAGE_LIMIT
                    && !messageDate.equals(currentDate)) {
                break;
            }

            selectedMessages.add(message);
            currentDate = messageDate;
        }

        return List.copyOf(selectedMessages);
    }

    private <T> List<T> reverseCopy(
            List<T> items
    ) {
        List<T> reversedItems = new ArrayList<>(items);
        Collections.reverse(reversedItems);
        return List.copyOf(reversedItems);
    }
}
