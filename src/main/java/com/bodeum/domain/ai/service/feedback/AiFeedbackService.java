package com.bodeum.domain.ai.service.feedback;

import com.bodeum.domain.ai.dto.request.CreateAiFeedbackRequest;
import com.bodeum.domain.ai.dto.response.CreateAiFeedbackResponse;
import com.bodeum.domain.ai.entity.AiFeedback;
import com.bodeum.domain.ai.entity.AiFeedbackReason;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiFeedbackReasonRepository;
import com.bodeum.domain.ai.repository.AiFeedbackRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 답변에 대한 사용자 피드백을 저장하고,
 * 반복된 오류 신고를 출처 검토 상태에 반영한다.
 */
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private static final int SOURCE_REVIEW_THRESHOLD = 3;

    private final AiMessageRepository aiMessageRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiFeedbackReasonRepository aiFeedbackReasonRepository;
    private final AiSourceReviewRepository aiSourceReviewRepository;

    @Transactional
    public CreateAiFeedbackResponse createFeedback(
            Long userId,
            Long aiMessageId,
            CreateAiFeedbackRequest request
    ) {
        AiMessage message = findMessageForFeedback(aiMessageId);

        if (!message.getChatRoom().getUser().getId().equals(userId)) {
            throw new ProjectException(AiErrorCode.AI_MESSAGE_FORBIDDEN);
        }
        if (message.getSenderType() != SenderType.AI) {
            throw new ProjectException(AiErrorCode.AI_FEEDBACK_AI_MESSAGE_ONLY);
        }
        if (message.getAiAnswerStatus() == AiAnswerStatus.GREETING) {
            throw new ProjectException(AiErrorCode.AI_FEEDBACK_NOT_ALLOWED);
        }
        if (aiFeedbackRepository.existsByAiMessageId(aiMessageId)) {
            throw new ProjectException(AiErrorCode.ALREADY_FEEDBACK);
        }

        AiFeedback feedback = aiFeedbackRepository.save(
                AiFeedback.create(message, request.feedbackType())
        );

        List<AiFeedbackReasonType> reasons = switch (request.feedbackType()) {
            case HELPFUL -> List.of();
            case INCORRECT -> request.reasons();
        };
        aiFeedbackReasonRepository.saveAll(reasons.stream()
                .map(reason -> AiFeedbackReason.create(feedback, reason))
                .toList());
        if (request.feedbackType() == AiFeedbackType.INCORRECT
                && message.getAiAnswerStatus() == AiAnswerStatus.ANSWERED) {
            updateSourceReviewStatus(aiMessageId);
        }

        return CreateAiFeedbackResponse.of(
                feedback.getId(),
                feedback.getFeedbackType(),
                reasons
        );
    }

    private AiMessage findMessageForFeedback(Long aiMessageId) {
        try {
            return aiMessageRepository.findByIdForFeedback(aiMessageId)
                    .orElseThrow(() -> new ProjectException(
                            AiErrorCode.AI_MESSAGE_NOT_FOUND));
        } catch (PessimisticLockingFailureException | QueryTimeoutException exception) {
            throw new ProjectException(
                    AiErrorCode.AI_FEEDBACK_TEMPORARILY_UNAVAILABLE,
                    exception
            );
        }
    }

    // 현재 답변의 출처별 오답 피드백 누적 횟수를 확인해 기준 이상이면 검토 필요 상태로 변경
    private void updateSourceReviewStatus(Long aiMessageId) {
        try {
            aiSourceReviewRepository.markReviewRequiredByIncorrectFeedbackCount(
                    aiMessageId,
                    SOURCE_REVIEW_THRESHOLD
            );
        } catch (PessimisticLockingFailureException | QueryTimeoutException exception) {
            throw new ProjectException(
                    AiErrorCode.AI_FEEDBACK_TEMPORARILY_UNAVAILABLE,
                    exception
            );
        }
    }
}
