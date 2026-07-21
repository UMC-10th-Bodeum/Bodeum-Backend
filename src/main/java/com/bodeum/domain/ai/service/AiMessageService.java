package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.AiMessageResponse;
import com.bodeum.domain.ai.dto.response.AiMessageSourceResponse;
import com.bodeum.domain.ai.dto.response.AiMessageWarningResponse;
import com.bodeum.domain.ai.dto.response.CreateAiMessageResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiWarningType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserAgreementRepository;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {

    private static final String NO_RESULT_MESSAGE = "관련 정보를 찾을 수 없습니다.";
    private static final String INCORRECT_WARNING =
            "일부 사용자로부터 오류 피드백이 접수된 정보입니다. 정확한 내용은 공식 기관에서 다시 확인해 주세요.";

    private final AiChatRoomRepository aiChatRoomRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final UserRepository userRepository;
    private final AiDocumentRetriever documentRetriever;
    private final AiAnswerGenerator answerGenerator;
    private final AiExternalAnswerProvider externalAnswerProvider;
    private final AiMessagePersistenceService persistenceService;
    private final AiMessageFailureService failureService;
    private final AiSourceReviewRepository aiSourceReviewRepository;
    private final AiRequestGuard requestGuard;
    private final AiReferenceDocumentResolver referenceDocumentResolver;

    public CreateAiMessageResponse createMessage(Long userId, String content) {
        validateAiTermsAgreement(userId);
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));
        try (AiRequestGuard.Permit ignored = requestGuard.acquire(userId, chatRoom.getId())) {
            return createMessage(chatRoom, userId, content);
        }
    }

    private CreateAiMessageResponse createMessage(
            AiChatRoom chatRoom,
            Long userId,
            String content
    ) {
        log.info("[AI] 사용자 프로필 조회 시작");

        User user = userRepository.findAiProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        User userWithDisabilities = userRepository.findAiDisabilityProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        log.info("[AI] 사용자 프로필 조회 완료");

        AiMessage userMessage = persistenceService.saveProcessingUserMessage(chatRoom, content);

        try {
            return generateAndSaveResponse(chatRoom, userMessage, content, user, userWithDisabilities);
        } catch (Exception e) {
            markFailedSafely(userMessage.getId(), e);
            throw e;
        }
    }

    private CreateAiMessageResponse generateAndSaveResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content,
            User user,
            User userWithDisabilities
    ) {

        AiUserProfile profile = toProfile(user, userWithDisabilities);

        log.info("[AI] 문서 검색 시작");
        List<AiReferenceDocument> retrievedDocuments = referenceDocumentResolver.resolve(
                documentRetriever.retrieve(content, profile)
        );

        log.info("[AI] 검색 문서 수: {}", retrievedDocuments.size());
        log.info("[AI] 검색 documentKeys: {}",
                retrievedDocuments.stream()
                        .map(AiReferenceDocument::documentKey)
                        .toList());

        if (retrievedDocuments.isEmpty()) {
            log.info("[AI] 내부 문서 없음, 외부 검색 시작");
            return createExternalOrNoResultResponse(chatRoom, userMessage, content, profile);
        }

        log.info("[AI] 답변 생성 시작");

        GeneratedAiAnswer generated = answerGenerator.generate(
                content, profile, retrievedDocuments
        );

        log.info("[AI] 답변 생성 완료");
        log.info("[AI] citedDocumentKeys: {}", generated.citedDocumentKeys());

        List<AiReferenceDocument> citedSources =
                validateCitations(generated, retrievedDocuments);

        if (citedSources.isEmpty()) {
            return createNoEvidenceResponse(chatRoom, userMessage);
        }

        boolean warning = hasIncorrectFeedback(citedSources);
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, generated.answer(), warning, citedSources);

        return sourceBackedResponse(
                message, citedSources, warningResponse(warning), AiAnswerStatus.ANSWERED);
    }

    private CreateAiMessageResponse createExternalOrNoResultResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String question,
            AiUserProfile profile
    ) {
        ExternalAiAnswer externalAnswer = externalAnswerProvider.search(question, profile);
        if (!externalAnswer.hasEvidence()) {
            return createNoEvidenceResponse(chatRoom, userMessage);
        }

        boolean warning = hasIncorrectFeedback(externalAnswer.sources());
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, externalAnswer.answer(), warning,
                externalAnswer.sources());
        return sourceBackedResponse(
                message,
                externalAnswer.sources(),
                warningResponse(warning),
                externalAnswer.answerStatus()
        );
    }

    private CreateAiMessageResponse createNoEvidenceResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, NO_RESULT_MESSAGE, false, List.of());
        return new CreateAiMessageResponse(AiMessageResponse.noEvidence(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        ));
    }

    private List<AiReferenceDocument> validateCitations(
            GeneratedAiAnswer generated,
            List<AiReferenceDocument> retrievedDocuments
    ) {
        log.info("[AI] citation 검증 시작");

        Set<String> citedKeys = new HashSet<>(
                generated.citedDocumentKeys() == null
                        ? List.of()
                        : generated.citedDocumentKeys()
        );

        List<AiReferenceDocument> cited = retrievedDocuments.stream()
                .filter(document -> citedKeys.contains(document.documentKey()))
                .toList();

        log.info("[AI] 유효 citation 수: {}", cited.size());

        if (cited.isEmpty()) {
            log.warn(
                    "[AI] citation 검증 실패. citedKeys={}, retrievedKeys={}",
                    citedKeys,
                    retrievedDocuments.stream()
                            .map(AiReferenceDocument::documentKey)
                            .toList()
            );

        }

        return cited;
    }

    private boolean hasIncorrectFeedback(List<AiReferenceDocument> sources) {
        if (sources.isEmpty()) {
            return false;
        }
        Set<AiSourceKey> sourceKeys = sources.stream()
                .map(source -> new AiSourceKey(source.sourceType(), source.sourceId()))
                .collect(java.util.stream.Collectors.toSet());
        return aiSourceReviewRepository.existsConfirmedIncorrectBySources(sourceKeys);
    }

    private AiUserProfile toProfile(
            User user,
            User disabilityProfileUser
    ) {
        return new AiUserProfile(
                user.getRegion() == null ? null : user.getRegion().getFullName(),
                user.getChildAge(),
                disabilityProfileUser.getDisabilityTypes().stream()
                        .map(Enum::name)
                        .toList(),
                user.getInterestCategories().stream()
                        .map(Enum::name)
                        .toList(),
                user.getKeywordText()
        );
    }

    private CreateAiMessageResponse sourceBackedResponse(
            AiMessage message,
            List<AiReferenceDocument> sources,
            AiMessageWarningResponse warning,
            AiAnswerStatus answerStatus
    ) {
        List<AiMessageSourceResponse> sourceResponses = sources.stream()
                .map(source -> new AiMessageSourceResponse(
                        source.sourceType(), source.sourceId(), source.title(),
                        source.url(), source.updatedAt()))
                .toList();
        AiMessageResponse response = AiMessageResponse.sourceBacked(
                message.getId(),
                message.getSenderType(),
                answerStatus,
                message.getContent(),
                message.getCreatedAt(),
                sourceResponses,
                warning);
        return new CreateAiMessageResponse(response);
    }

    private AiMessageWarningResponse warningResponse(boolean warning) {
        return warning
                ? new AiMessageWarningResponse(AiWarningType.INCORRECT_SOURCE, INCORRECT_WARNING)
                : null;
    }

    private void markFailedSafely(Long userMessageId, Exception originalException) {
        try {
            failureService.markFailed(userMessageId);
        } catch (Exception failureStatusException) {
            originalException.addSuppressed(failureStatusException);
            log.error("Failed to mark AI user message as FAILED: userMessageId={}",
                    userMessageId, failureStatusException);
        }
    }

    private void validateAiTermsAgreement(Long userId) {
        UserAgreement agreement = userAgreementRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_AGREEMENT_NOT_FOUND));
        if (!agreement.isAiTermsAgreed()) {
            throw new ProjectException(AiErrorCode.AI_TERMS_NOT_AGREED);
        }
    }
}
