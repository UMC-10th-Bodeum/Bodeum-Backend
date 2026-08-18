package com.bodeum.domain.ai.service.chat;

import com.bodeum.domain.ai.service.response.AiAnswerFallbackService;
import com.bodeum.domain.ai.service.response.AiAnswerResultNormalizer;
import com.bodeum.domain.ai.service.response.AiResourceListAnswerBuilder;
import com.bodeum.domain.ai.service.response.AiStarterQuestionRouter;
import com.bodeum.domain.ai.service.retrieval.AiDocumentSearchService;
import com.bodeum.domain.ai.service.retrieval.AiResourceListSearchService;
import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;
import com.bodeum.domain.ai.service.validation.AiSiteListAnswerValidator;
import com.bodeum.domain.ai.service.context.AiConversationContextService;
import com.bodeum.domain.ai.service.context.AiQuestionContextResolver;
import com.bodeum.domain.ai.service.context.AiQuestionRegionResolver;
import com.bodeum.domain.ai.service.context.AiQuestionSearchQueryBuilder;
import com.bodeum.domain.ai.service.context.AiStarterQuestionContextResolver;
import com.bodeum.domain.ai.service.context.AiUserProfileFactory;
import com.bodeum.domain.ai.service.support.AiRequestGuard;
import com.bodeum.domain.ai.service.support.AiScrapInterestService;
import com.bodeum.domain.ai.util.AiTextNormalizer;

import com.bodeum.domain.ai.dto.response.*;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.context.AiQuestionContext;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.context.AiSearchQueryContext;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 사용자 질문을 분석하고 내부·외부 근거를 검색하여 AI 답변을 생성하며,
 * 응답 검증과 메시지 저장까지 전체 처리 흐름을 조율한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {

    private static final Pattern RELATIVE_LOCAL_REGION_PATTERN = Pattern.compile(
            "(우리\\s*(지역|동네)|근처|주변)");
    private final AiChatRoomRepository aiChatRoomRepository;
    private final UserRepository userRepository;
    private final AiAnswerGenerator answerGenerator;
    private final AiMessagePersistenceService persistenceService;
    private final AiMessageFailureService failureService;
    private final AiRequestGuard requestGuard;
    private final AiStarterQuestionRouter starterQuestionRouter;
    private final AiScrapInterestService scrapInterestService;
    private final AiQuestionRegionResolver questionRegionResolver;
    private final AiSiteListAnswerValidator siteListAnswerValidator;
    private final AiDocumentSearchService documentSearchService;
    private final AiQuestionContextResolver questionContextResolver;
    private final AiConversationContextService conversationContextService;
    private final AiQuestionSearchQueryBuilder searchQueryBuilder;
    private final AiAnswerEvidenceService evidenceService;
    private final AiAnswerFallbackService fallbackService;
    private final AiUserProfileFactory userProfileFactory;
    private final AiStarterQuestionContextResolver starterQuestionContextResolver;
    private final AiAnswerResultNormalizer answerResultNormalizer;
    private final AiResourceListSearchService resourceListSearchService;
    private final AiResourceListAnswerBuilder resourceListAnswerBuilder;

    /**
     * AI 질문 요청 제한을 검증한 뒤 응답 생성 흐름을 시작한다.
     */
    public CreateAiMessageResponse createMessage(Long userId, String content) {
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
        log.debug("[AI] 사용자 프로필 조회 시작");

        // AI 개인화에 사용할 사용자 프로필과 최근 관심 정보 조회
        User user = userRepository.findAiProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        User userWithDisabilities = userRepository.findAiDisabilityProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        AiScrapInterests scrapInterests = loadScrapInterestsSafely(userId);

        log.debug("[AI] 사용자 프로필 조회 완료");

        // AI 응답 생성 전 사용자 질문을 PROCESSING 상태로 저장
        AiMessage userMessage = persistenceService.saveProcessingUserMessage(chatRoom, content);

        // 응답 생성 실패 시 사용자 메시지를 FAILED 상태로 변경
        try {
            return generateAndSaveResponse(
                    chatRoom,
                    userMessage,
                    content,
                    user,
                    userWithDisabilities,
                    scrapInterests
            );
        } catch (Exception e) {
            logResponseGenerationFailure(userId, chatRoom.getId(), userMessage.getId(), e);
            markFailedSafely(userMessage.getId(), e);
            throw e;
        }
    }

    private CreateAiMessageResponse generateAndSaveResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content,
            User user,
            User userWithDisabilities,
            AiScrapInterests scrapInterests
    ) {
        // 최근 대화 문맥과 사용자 개인화 프로필 구성
        AiConversationContext conversationContext =
                conversationContextService.resolve(chatRoom.getId());
        AiUserProfile baseProfile = userProfileFactory.create(
                user, userWithDisabilities, scrapInterests);

        // 상대적 지역 표현을 사용했지만 프로필 지역이 없으면 지역 입력 요청
        if (RELATIVE_LOCAL_REGION_PATTERN.matcher(content).find()
                && (baseProfile.region() == null || baseProfile.region().isBlank())) {
            persistenceService.updateUserMessageContext(
                    userMessage.getId(), content, null, null, userMessage.getId());
            return fallbackService.regionRequired(
                    chatRoom,
                    userMessage,
                    "어느 지역을 기준으로 찾을까요? 시·도와 시·군·구를 알려주세요."
            );
        }

        // 지역 시설 질문의 지역이 여러 곳으로 해석되면, 사용자에게 지역 확인 요청
        AiQuestionRegionResolver.RegionResolution regionResolution =
                questionRegionResolver.resolve(content, baseProfile);
        if (isLocalResourceTarget(content)
                && regionResolution.isAmbiguous()) {
            persistenceService.updateUserMessageContext(
                    userMessage.getId(), content, null, null, userMessage.getId());
            return fallbackService.regionRequired(
                    chatRoom, userMessage, regionResolution.ambiguityMessage());
        }

        // 질문 의도와 이전 대화를 분석해 검색에 사용할 질문 문맥 구성
        AiQuestionContext questionContext = resolveStarterOrAnalyzedQuestionContext(
                chatRoom.getId(),
                content,
                baseProfile,
                conversationContext
        );

        // 추가 결과 요청이면, 이전 답변의 출처를 제외하기 위한 문맥 구성
        AiAdditionalResultsContext additionalResultsContext =
                conversationContextService.resolveAdditionalResults(
                        chatRoom.getId(), conversationContext,
                        questionContext.excludePreviousResults());
        String resolvedContent = questionContext.resolvedQuestion() == null
                ? content
                : questionContext.resolvedQuestion();
        if (additionalResultsContext.isFollowUp()
                && !normalizeQuestion(resolvedContent).contains(normalizeQuestion(
                        additionalResultsContext.previousQuestion()))) {
            resolvedContent = additionalResultsContext.previousQuestion()
                    + "\n이전에 안내한 항목을 제외하고 " + resolvedContent;
        }
        boolean followUp = questionContext.followUp()
                || additionalResultsContext.isFollowUp();

        // 해석된 질문과 부모·루트 메시지 정보를 저장해 후속 질문 연결 관계 유지
        persistenceService.updateUserMessageContext(
                userMessage.getId(),
                resolvedContent,
                questionContext.resolvedContext(),
                followUp ? conversationContext.parentUserMessageId() : null,
                !followUp || conversationContext.rootUserMessageId() == null
                        ? userMessage.getId()
                        : conversationContext.rootUserMessageId()
        );

        // 안전 안내가 필요한 질문은 일반 검색 없이 안내 응답 반환
        if (questionContext.safetyGuidance().isPresent()) {
            log.info("[AI] 안전 응답 안내로 전환");
            return fallbackService.noEvidence(
                    chatRoom,
                    userMessage,
                    questionContext.safetyGuidance().get()
            );
        }

        // 질문 의도가 불명확하면 검색 전에 사용자에게 추가 정보 요청
        if (questionContext.needsClarification()) {
            log.info("[AI] 사용자 확인 질문으로 전환");
            return fallbackService.clarificationRequired(
                    chatRoom,
                    userMessage,
                    questionContext.clarificationQuestion()
            );
        }

        // 추가 결과 요청 시 이전 검색 카테고리를 유지
        AiUserProfile searchProfile = questionContext.searchProfile();
        if (additionalResultsContext.isFollowUp()) {
            InfoSubCategory previousCategory = questionContextResolver.resolveInfoSubCategory(
                    additionalResultsContext.previousQuestion());
            if (previousCategory != null) {
                searchProfile = searchProfile.withInfoSubCategory(previousCategory);
            }
        }
        AiUserProfile effectiveSearchProfile = searchProfile;

        // 초기 추천 질문은 사전 정의된 답변을 우선 조회하고, 근거가 없으면 일반 검색으로 전환
        Optional<AiStarterQuestionAnswer> starterAnswer =
                questionContext.curatedAnswerType()
                        .flatMap(type -> questionContext.requestedResultCount() == null
                                ? starterQuestionRouter.route(
                                        type, effectiveSearchProfile)
                                : starterQuestionRouter.route(
                                        type,
                                        effectiveSearchProfile,
                                        questionContext.requestedResultCount()));
        if (starterAnswer.isPresent()) {
            AiStarterQuestionAnswer answer = starterAnswer.get();
            if (answer.isRegionRequired() || answer.hasEvidence()) {
                return fallbackService.saveStarterAnswer(chatRoom, userMessage, answer);
            }
            log.info("[AI] 추천 질문 출처 없음, 일반 질문 흐름으로 전환");
        }

        // 지역·결과 개수·후속 질문 문맥을 반영한 최종 검색 쿼리 구성
        AiSearchQueryContext searchContext = searchQueryBuilder.build(
                resolvedContent, questionContext.retrievalQueries(), searchProfile,
                questionContext.searchScope(), questionContext.requestedResultCount(),
                additionalResultsContext);
        String searchQuestion = searchContext.question();
        List<String> searchQueries = searchContext.queries();

        log.debug("[AI] 문서 검색 시작");
        // 구조화된 시설 목록 요청은 전용 검색을 우선 수행
        boolean structuredResourceList = questionContext.isResourceListRequest()
                || (additionalResultsContext.isFollowUp()
                && searchProfile.infoSubCategory() != null);
        List<AiReferenceDocument> retrievedDocuments = structuredResourceList
                ? resourceListSearchService.retrieve(
                        searchProfile,
                        questionContext.searchScope(),
                        questionContext.requestedResultCount(),
                        additionalResultsContext.excludedSources(),
                        additionalResultsContext.excludedIdentityKeys())
                : List.of();
        boolean hasStructuredResourceList = !retrievedDocuments.isEmpty();

        // 전용 검색 결과가 없으면, 일반 문서 검색 수행
        if (retrievedDocuments.isEmpty()) {
            retrievedDocuments = retrieveDocuments(
                        searchQuestion,
                        searchQueries,
                        questionContext.searchGoal(),
                        questionContext.requiredConcepts(),
                        searchProfile,
                        questionContext.searchScope(),
                        questionContext.requestedResultCount(),
                        additionalResultsContext);
        }

        // 추가 결과 요청에서는 이전 답변에 사용된 출처와 동일 기관을 제외
        retrievedDocuments = retrievedDocuments.stream()
                .filter(document -> !additionalResultsContext.excludedSources().contains(
                        new AiSourceKey(document.sourceType(), document.sourceId())))
                .filter(document -> evidenceService.documentIdentityKeys(document).stream()
                        .noneMatch(additionalResultsContext.excludedIdentityKeys()::contains))
                .toList();
        if (additionalResultsContext.isFollowUp()) {
            retrievedDocuments = evidenceService.deduplicateInstitutions(retrievedDocuments);
        }

        // MySQL의 구조화된 기관 목록은 LLM을 거치지 않고 정확한 항목과 출처로 응답
        if (hasStructuredResourceList && !retrievedDocuments.isEmpty()) {
            String answer = resourceListAnswerBuilder.build(
                    retrievedDocuments,
                    questionContext.requestedResultCount(),
                    additionalResultsContext.isFollowUp(),
                    searchProfile.infoSubCategory());
            boolean warning = evidenceService.hasIncorrectFeedback(retrievedDocuments);
            AiMessage message = persistenceService.saveAiMessageAndComplete(
                    userMessage.getId(), chatRoom, answer, warning,
                    AiAnswerStatus.ANSWERED, retrievedDocuments);
            return fallbackService.sourceBacked(
                    message, retrievedDocuments, AiAnswerStatus.ANSWERED);
        }

        log.info("[AI] 검색 문서 수: {}", retrievedDocuments.size());
        log.debug("[AI] 검색 documentKeys: {}",
                retrievedDocuments.stream()
                        .map(AiReferenceDocument::documentKey)
                        .toList());

        // 내부 근거를 찾지 못하면 허용된 외부 출처 검색으로 전환
        if (retrievedDocuments.isEmpty()) {
            log.info("[AI] 내부 문서 없음, 외부 검색 시작");
            return fallbackService.externalOrNoResult(
                    chatRoom,
                    userMessage,
                    searchQuestion,
                    searchQueries,
                    searchProfile,
                    questionContext.searchScope()
            );
        }

        log.debug("[AI] 답변 생성 시작");

        // 검색된 근거 문서를 기반으로 AI 답변 생성 및 요청 개수에 맞게 결과 보정
        GeneratedAiAnswer generated = answerGenerator.generate(
                content, resolvedContent, questionContext.resolvedContext(),
                searchProfile.region(), questionContext.userProfile(), retrievedDocuments
        );
        generated = answerResultNormalizer.normalizeListedResultCount(
                generated,
                questionContext.requestedResultCount(),
                additionalResultsContext.isFollowUp(),
                searchProfile.infoSubCategory()
        );

        log.debug("[AI] 답변 생성 완료");
        log.debug("[AI] citedDocumentKeys: {}", generated.citedDocumentKeys());

        // 사이트 목록 답변이 검색 근거와 일치하지 않으면 외부 검색으로 전환
        if (questionContext.isSiteListRequest()
                && !siteListAnswerValidator.isValid(generated, retrievedDocuments)) {
            log.warn("[AI] 사이트 목록 항목과 인용 근거 검증 실패, 외부 검색 시작");
            return fallbackService.externalOrNoResult(
                    chatRoom,
                    userMessage,
                    searchQuestion,
                    searchQueries,
                    searchProfile,
                    questionContext.searchScope()
            );
        }

        List<AiReferenceDocument> citedSources =
                evidenceService.validateCitations(generated, retrievedDocuments);

        // 검색 결과가 있더라도 LLM이 그 문서를 실제 근거로 인용하지 않았다면,
        // 내부 답변을 폐기하고 등록된 외부 사이트 범위에서 근거를 다시 찾는다.
        if (citedSources.isEmpty()) {
            log.info("[AI] 내부 문서 인용 근거 없음, 외부 검색 시작");
            return fallbackService.externalOrNoResult(
                    chatRoom,
                    userMessage,
                    searchQuestion,
                    searchQueries,
                    searchProfile,
                    questionContext.searchScope()
            );
        }

        boolean warning = evidenceService.hasIncorrectFeedback(citedSources);
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, generated.answer(), warning,
                AiAnswerStatus.ANSWERED, citedSources);

        return fallbackService.sourceBacked(message, citedSources, AiAnswerStatus.ANSWERED);
    }

    /**
     * 초기 추천 질문 전용 문맥을 우선 적용하고,
     * 해당하지 않으면 일반 질문 분석 문맥을 생성한다.
     */
    private AiQuestionContext resolveStarterOrAnalyzedQuestionContext(
            Long chatRoomId,
            String content,
            AiUserProfile profile,
            AiConversationContext conversationContext
    ) {
        return starterQuestionContextResolver.resolve(chatRoomId, content, profile)
                .orElseGet(() -> questionContextResolver.resolve(
                        content, profile, conversationContext));
    }

    private boolean isLocalResourceTarget(String question) {
        return questionContextResolver.isLocalResourceTarget(question);
    }

    private String normalizeQuestion(String content) {
        return AiTextNormalizer.removeWhitespace(
                AiTextNormalizer.normalizeQuestionSpacing(content));
    }

    private List<AiReferenceDocument> retrieveDocuments(
            String originalQuestion,
            List<String> expandedQueries,
            String searchGoal,
            List<AiRequiredConcept> requiredConcepts,
            AiUserProfile profile,
            AiSearchScope searchScope,
            Integer requestedResultCount,
            AiAdditionalResultsContext additionalResultsContext
    ) {
        return documentSearchService.retrieve(
                originalQuestion,
                expandedQueries,
                searchGoal,
                requiredConcepts,
                profile,
                searchScope,
                requestedResultCount,
                additionalResultsContext
        );
    }

    // 스크랩 조회 실패가 AI 질문 전체 실패로 이어지지 않도록 빈 관심 정보로 대체
    private AiScrapInterests loadScrapInterestsSafely(Long userId) {
        try {
            return scrapInterestService.findRecentInterests(userId);
        } catch (Exception e) {
            log.warn("[AI] 최근 스크랩 관심 정보 조회 실패, 기본 프로필로 처리합니다.", e);
            return AiScrapInterests.empty();
        }
    }

    // 실패 상태 기록 중 추가 예외가 발생해도 원래 응답 생성 예외를 유지
    private void markFailedSafely(Long userMessageId, Exception originalException) {
        try {
            failureService.markFailed(userMessageId);
        } catch (Exception failureStatusException) {
            originalException.addSuppressed(failureStatusException);
            log.error("Failed to mark AI user message as FAILED: userMessageId={}",
                    userMessageId, failureStatusException);
        }
    }

    private void logResponseGenerationFailure(
            Long userId,
            Long chatRoomId,
            Long userMessageId,
            Exception exception
    ) {
        Throwable rootCause = findRootCause(exception);
        String errorCode = exception instanceof ProjectException projectException
                ? projectException.getErrorCode().getCode()
                : "UNEXPECTED_ERROR";

        log.error(
                "[AI] 응답 생성 실패: userId={}, chatRoomId={}, userMessageId={}, "
                        + "errorCode={}, exceptionType={}, rootCauseType={}",
                userId,
                chatRoomId,
                userMessageId,
                errorCode,
                exception.getClass().getName(),
                rootCause.getClass().getName(),
                exception
        );
    }

    private Throwable findRootCause(Throwable exception) {
        Throwable rootCause = exception;
        while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
            rootCause = rootCause.getCause();
        }
        return rootCause;
    }

}
