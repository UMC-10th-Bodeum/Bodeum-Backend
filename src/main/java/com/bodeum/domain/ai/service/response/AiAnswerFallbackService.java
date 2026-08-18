package com.bodeum.domain.ai.service.response;

import com.bodeum.domain.ai.service.chat.AiMessagePersistenceService;

import com.bodeum.domain.ai.dto.response.AiMessageResponse;
import com.bodeum.domain.ai.dto.response.AiMessageSourceResponse;
import com.bodeum.domain.ai.dto.response.AiMessageWarningResponse;
import com.bodeum.domain.ai.dto.response.CreateAiMessageResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 내부 근거가 부족하거나 추가 확인이 필요한 경우
 * 외부 검색 또는 상태별 대체 응답을 생성하고 저장한다.
 */
@Service
@RequiredArgsConstructor
public class AiAnswerFallbackService {

    private static final String NO_RESULT_MESSAGE = "관련 정보를 찾을 수 없습니다.";

    private final AiExternalAnswerProvider externalAnswerProvider;
    private final AiMessagePersistenceService persistenceService;
    private final AiAnswerEvidenceService evidenceService;

    public CreateAiMessageResponse saveStarterAnswer(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            AiStarterQuestionAnswer answer
    ) {
        if (answer.isRegionRequired()) {
            return regionRequired(chatRoom, userMessage, answer.content());
        }
        if (!answer.hasEvidence()) {
            return noEvidence(chatRoom, userMessage);
        }
        return saveSourceBacked(chatRoom, userMessage, answer.content(),
                AiAnswerStatus.ANSWERED, answer.sources());
    }

    public CreateAiMessageResponse externalOrNoResult(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        ExternalAiAnswer answer = externalAnswerProvider.search(
                question, retrievalQueries, profile, searchScope);
        if (!answer.hasEvidence()) {
            String content = answer.answer() == null || answer.answer().isBlank()
                    ? NO_RESULT_MESSAGE : answer.answer();
            return noEvidence(chatRoom, userMessage, content);
        }
        return saveSourceBacked(chatRoom, userMessage, answer.answer(),
                answer.answerStatus(), answer.sources());
    }

    public CreateAiMessageResponse regionRequired(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, content, false,
                AiAnswerStatus.REGION_REQUIRED, List.of());
        return new CreateAiMessageResponse(AiMessageResponse.regionRequired(
                message.getId(), message.getSenderType(), message.getContent(),
                message.getCreatedAt()));
    }

    public CreateAiMessageResponse clarificationRequired(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, content, false,
                AiAnswerStatus.CLARIFICATION_REQUIRED, List.of());
        return new CreateAiMessageResponse(AiMessageResponse.clarificationRequired(
                message.getId(), message.getSenderType(), message.getContent(),
                message.getCreatedAt()));
    }

    public CreateAiMessageResponse noEvidence(AiChatRoom chatRoom, AiMessage userMessage) {
        return noEvidence(chatRoom, userMessage, NO_RESULT_MESSAGE);
    }

    public CreateAiMessageResponse noEvidence(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, content, false,
                AiAnswerStatus.NO_EVIDENCE, List.of());
        return new CreateAiMessageResponse(AiMessageResponse.noEvidence(
                message.getId(), message.getSenderType(), message.getContent(),
                message.getCreatedAt()));
    }

    public CreateAiMessageResponse sourceBacked(
            AiMessage message,
            List<AiReferenceDocument> sources,
            AiAnswerStatus answerStatus
    ) {
        boolean warning = evidenceService.hasIncorrectFeedback(sources);
        List<AiMessageSourceResponse> sourceResponses = sources.stream()
                .map(source -> new AiMessageSourceResponse(
                        source.sourceType(), source.sourceId(), source.title(),
                        source.url(), source.updatedAt()))
                .toList();
        AiMessageResponse response = AiMessageResponse.sourceBacked(
                message.getId(), message.getSenderType(), answerStatus,
                message.getContent(), message.getCreatedAt(), sourceResponses,
                warning ? AiMessageWarningResponse.incorrectSource() : null);
        return new CreateAiMessageResponse(response);
    }

    private CreateAiMessageResponse saveSourceBacked(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content,
            AiAnswerStatus answerStatus,
            List<AiReferenceDocument> sources
    ) {
        boolean warning = evidenceService.hasIncorrectFeedback(sources);
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, content, warning, answerStatus, sources);
        return sourceBacked(message, sources, answerStatus);
    }
}
