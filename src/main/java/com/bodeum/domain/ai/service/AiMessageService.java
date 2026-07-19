package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.AiMessageResponse;
import com.bodeum.domain.ai.dto.response.AiMessageSourceResponse;
import com.bodeum.domain.ai.dto.response.CreateAiMessageResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiSourceReviewStatus;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.AiReferenceDocument;
import com.bodeum.domain.ai.model.AiUserProfile;
import com.bodeum.domain.ai.model.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.ExternalAiAnswer;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiMessageService {

    private static final String NO_RESULT_MESSAGE = "관련 정보를 찾을 수 없습니다.";
    private static final String DISCLAIMER =
            "AI 답변은 참고용이며, 정확한 내용은 공식 기관에서 최종 확인해 주세요.";
    private static final String INCORRECT_WARNING =
            "일부 사용자로부터 오류 피드백이 접수된 정보입니다. 공식 기관에서 다시 확인해 주세요.";

    private final AiChatRoomRepository aiChatRoomRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final UserRepository userRepository;
    private final AiDocumentRetriever documentRetriever;
    private final AiAnswerGenerator answerGenerator;
    private final AiExternalAnswerProvider externalAnswerProvider;
    private final AiMessagePersistenceService persistenceService;
    private final AiSourceReviewRepository aiSourceReviewRepository;
    private final AiRequestGuard requestGuard;

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
        User user = userRepository.findAiProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        persistenceService.saveUserMessage(chatRoom, content);
        AiUserProfile profile = toProfile(user);
        List<AiReferenceDocument> retrievedDocuments = documentRetriever.retrieve(content, profile);

        if (retrievedDocuments.isEmpty()) {
            return createExternalOrNoResultResponse(chatRoom, content, profile);
        }

        GeneratedAiAnswer generated = answerGenerator.generate(
                content, profile, retrievedDocuments);
        List<AiReferenceDocument> citedSources = validateCitations(generated, retrievedDocuments);
        boolean warning = citedSources.stream().anyMatch(this::hasIncorrectFeedback);
        AiMessage message = persistenceService.saveAiMessage(
                chatRoom, generated.answer(), warning, citedSources);

        return response(message, citedSources, warning ? INCORRECT_WARNING : null);
    }

    private CreateAiMessageResponse createExternalOrNoResultResponse(
            AiChatRoom chatRoom,
            String question,
            AiUserProfile profile
    ) {
        ExternalAiAnswer externalAnswer = externalAnswerProvider.search(question, profile);
        if (!externalAnswer.hasEvidence()) {
            AiMessage message = persistenceService.saveAiMessage(
                    chatRoom, NO_RESULT_MESSAGE, false, List.of());
            return response(message, List.of(), null);
        }

        boolean warning = externalAnswer.sources().stream().anyMatch(this::hasIncorrectFeedback);
        AiMessage message = persistenceService.saveAiMessage(
                chatRoom, externalAnswer.answer(), warning, externalAnswer.sources());
        return response(
                message,
                externalAnswer.sources(),
                warning ? INCORRECT_WARNING : null
        );
    }

    private List<AiReferenceDocument> validateCitations(
            GeneratedAiAnswer generated,
            List<AiReferenceDocument> retrievedDocuments
    ) {
        Set<String> citedKeys = new HashSet<>(
                generated.citedDocumentKeys() == null ? List.of() : generated.citedDocumentKeys());
        List<AiReferenceDocument> cited = retrievedDocuments.stream()
                .filter(document -> citedKeys.contains(document.documentKey()))
                .toList();
        if (cited.isEmpty()) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED);
        }
        return cited;
    }

    private boolean hasIncorrectFeedback(AiReferenceDocument source) {
        return aiSourceReviewRepository.existsBySourceTypeAndSourceIdAndReviewStatus(
                source.sourceType(),
                source.sourceId(),
                AiSourceReviewStatus.CONFIRMED_INCORRECT
        );
    }

    private AiUserProfile toProfile(User user) {
        return new AiUserProfile(
                user.getRegion() == null ? null : user.getRegion().getFullName(),
                user.getChildAge(),
                user.getDisabilityTypes().stream().map(Enum::name).toList(),
                user.getInterestCategories().stream().map(Enum::name).toList(),
                user.getKeywordText());
    }

    private CreateAiMessageResponse response(
            AiMessage message,
            List<AiReferenceDocument> sources,
            String warning
    ) {
        return new CreateAiMessageResponse(new AiMessageResponse(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt(),
                sources.stream()
                        .map(source -> new AiMessageSourceResponse(
                                source.sourceType(), source.sourceId(), source.title(),
                                source.url(), source.updatedAt()))
                        .toList(),
                warning,
                DISCLAIMER));
    }

    private void validateAiTermsAgreement(Long userId) {
        UserAgreement agreement = userAgreementRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_AGREEMENT_NOT_FOUND));
        if (!agreement.isAiTermsAgreed()) {
            throw new ProjectException(AiErrorCode.AI_TERMS_NOT_AGREED);
        }
    }
}
