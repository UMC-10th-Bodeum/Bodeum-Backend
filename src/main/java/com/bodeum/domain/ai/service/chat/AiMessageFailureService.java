package com.bodeum.domain.ai.service.chat;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 응답 생성 실패 상태를 기록하고,
 * 비정상적으로 처리 중 상태에 머문 메시지를 복구한다.
 */
@Service
@RequiredArgsConstructor
public class AiMessageFailureService {

    private final AiMessageRepository aiMessageRepository;

    /**
     * AI 응답 생성 실패를 기존 트랜잭션과 독립적으로 기록한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long userMessageId) {
        AiMessage userMessage = aiMessageRepository.findById(userMessageId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));
        userMessage.failAiResponse();
    }

    /**
     * 기준 시각 이전부터 처리 중인 사용자 메시지를 실패 상태로 일괄 변경한다.
     */
    @Transactional
    public int recoverStaleProcessingMessages(Instant cutoff) {
        return aiMessageRepository.markStaleProcessingMessages(
                SenderType.USER,
                AiResponseProcessingStatus.PROCESSING,
                AiResponseProcessingStatus.FAILED,
                cutoff
        );
    }
}
