package com.bodeum.domain.ai.service.chat;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.entity.AiResponseSource;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.support.AiSourceDeduplicator;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 대화 메시지와 출처를 저장하고,
 * 사용자 메시지의 처리 상태 및 대화 문맥을 함께 갱신한다.
 */
@Service
@RequiredArgsConstructor
public class AiMessagePersistenceService {

    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiChatRoomRepository aiChatRoomRepository;

    /**
     * 사용자 질문을 처리 중 상태의 메시지로 저장하고, 채팅방의 마지막 메시지 시각을 갱신한다.
     */
    @Transactional
    public AiMessage saveProcessingUserMessage(AiChatRoom chatRoom, String content) {
        AiMessage message = aiMessageRepository.save(AiMessage.createUserMessage(chatRoom, content));
        chatRoom.updateLastMessageAt(Instant.now());
        aiChatRoomRepository.save(chatRoom);
        return message;
    }

    /**
     * AI가 해석한 질문과 대화 연결 정보를 사용자 메시지에 저장한다.
     */
    @Transactional
    public void updateUserMessageContext(
            Long userMessageId,
            String resolvedQuestion,
            AiResolvedContext resolvedContext,
            Long contextParentMessageId,
            Long contextRootMessageId
    ) {
        rejectCancelledResponse();
        AiMessage userMessage = aiMessageRepository.findById(userMessageId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));
        userMessage.updateConversationContext(
                resolvedQuestion,
                resolvedContext,
                contextParentMessageId,
                contextRootMessageId
        );
    }

    /**
     * AI 답변과 출처를 저장하고,
     * 사용자 메시지의 응답 처리를 완료 상태로 변경한다.
     */
    @Transactional
    public AiMessage saveAiMessageAndComplete(
            Long userMessageId,
            AiChatRoom chatRoom,
            String content,
            boolean warning,
            AiAnswerStatus answerStatus,
            List<AiReferenceDocument> sources
    ) {
        rejectCancelledResponse();
        AiMessage userMessage = aiMessageRepository.findByIdForAiResponse(userMessageId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));
        if (Thread.currentThread().isInterrupted()
                || !userMessage.isAiResponseProcessing()) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT);
        }
        AiMessage message = aiMessageRepository.save(
                AiMessage.createAiMessage(chatRoom, content, warning, answerStatus));

        // 사용자 질문의 대화 연결 정보를 AI 답변에도 동일하게 적용
        message.inheritConversationContext(userMessage);

        // AI 답변에 사용된 근거 출처를 함께 저장
        List<AiReferenceDocument> distinctSources = AiSourceDeduplicator.deduplicate(sources);
        aiResponseSourceRepository.saveAll(distinctSources.stream()
                .map(source -> AiResponseSource.create(
                        message, source.sourceType(), source.sourceId(), source.title(),
                        source.url(), source.updatedAt()))
                .toList());

        // AI 답변 생성이 정상적으로 완료되었음을 사용자 메시지에 반영
        userMessage.completeAiResponse();

        chatRoom.updateLastMessageAt(Instant.now());
        aiChatRoomRepository.save(chatRoom);

        return message;
    }

    private void rejectCancelledResponse() {
        if (Thread.currentThread().isInterrupted()) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT);
        }
    }
}
