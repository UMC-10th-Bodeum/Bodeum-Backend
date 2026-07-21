package com.bodeum.domain.ai.service;

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

@Service
@RequiredArgsConstructor
public class AiMessageFailureService {

    private final AiMessageRepository aiMessageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long userMessageId) {
        AiMessage userMessage = aiMessageRepository.findById(userMessageId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));
        userMessage.failAiResponse();
    }

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
