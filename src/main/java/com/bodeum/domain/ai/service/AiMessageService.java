package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.*;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.enums.SenderType;
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
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {

    private static final Pattern RELATIVE_LOCAL_REGION_PATTERN = Pattern.compile(
            "(우리\\s*(지역|동네)|근처|주변)");
    private static final String AMBIGUOUS_REGION_MESSAGE_PREFIX =
            "확인할 지역이 여러 곳입니다. ";
    private static final Set<String> EXPLICIT_REGION_REHAB_QUESTIONS = Set.of(
            "재활센터추천해줘",
            "재활센터를추천해줘",
            "재활센터알려줘",
            "재활센터를알려줘"
    );
    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
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

        User user = userRepository.findAiProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        User userWithDisabilities = userRepository.findAiDisabilityProfileById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        AiScrapInterests scrapInterests = loadScrapInterestsSafely(userId);

        log.debug("[AI] 사용자 프로필 조회 완료");

        AiMessage userMessage = persistenceService.saveProcessingUserMessage(chatRoom, content);

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

        AiAdditionalResultsContext additionalResultsContext =
                conversationContextService.resolveAdditionalResults(chatRoom.getId(), content);
        AiConversationContext conversationContext =
                conversationContextService.resolve(chatRoom.getId());
        AiUserProfile baseProfile = toProfile(
                user, userWithDisabilities, scrapInterests);
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
        AiQuestionRegionResolver.RegionResolution regionResolution =
                questionRegionResolver.resolve(content, baseProfile);
        if (isLocalResourceTarget(content)
                && regionResolution.isAmbiguous()) {
            persistenceService.updateUserMessageContext(
                    userMessage.getId(), content, null, null, userMessage.getId());
            return fallbackService.regionRequired(
                    chatRoom, userMessage, regionResolution.ambiguityMessage());
        }

        AiQuestionContext questionContext = resolveQuestionContext(
                chatRoom.getId(),
                content,
                baseProfile,
                conversationContext
        );
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
        persistenceService.updateUserMessageContext(
                userMessage.getId(),
                resolvedContent,
                questionContext.resolvedContext(),
                followUp ? conversationContext.parentUserMessageId() : null,
                !followUp || conversationContext.rootUserMessageId() == null
                        ? userMessage.getId()
                        : conversationContext.rootUserMessageId()
        );
        if (questionContext.safetyGuidance().isPresent()) {
            log.info("[AI] 안전 응답 안내로 전환");
            return fallbackService.noEvidence(
                    chatRoom,
                    userMessage,
                    questionContext.safetyGuidance().get()
            );
        }
        if (questionContext.needsClarification()) {
            log.info("[AI] 사용자 확인 질문으로 전환");
            return fallbackService.clarificationRequired(
                    chatRoom,
                    userMessage,
                    questionContext.clarificationQuestion()
            );
        }

        AiUserProfile profile = questionContext.profile();
        Optional<AiStarterQuestionAnswer> starterAnswer =
                questionContext.requestedResultCount() == null
                        && shouldRouteStarterQuestion(questionContext)
                        ? questionContext.questionType()
                                .flatMap(type -> starterQuestionRouter.route(type, profile))
                        : Optional.empty();
        if (starterAnswer.isPresent()) {
            AiStarterQuestionAnswer answer = starterAnswer.get();
            if (answer.isRegionRequired() || answer.hasEvidence()) {
                return fallbackService.saveStarterAnswer(chatRoom, userMessage, answer);
            }
            log.info("[AI] 추천 질문 출처 없음, 일반 질문 흐름으로 전환");
        }

        AiSearchQueryContext searchContext = searchQueryBuilder.build(
                resolvedContent, questionContext.retrievalQueries(), profile,
                questionContext.searchScope(), questionContext.requestedResultCount(),
                additionalResultsContext);
        String searchQuestion = searchContext.question();
        List<String> searchQueries = searchContext.queries();

        log.debug("[AI] 문서 검색 시작");
        List<AiReferenceDocument> retrievedDocuments = retrieveDocuments(
                searchQuestion,
                searchQueries,
                questionContext.searchGoal(),
                questionContext.requiredConcepts(),
                profile,
                questionContext.searchScope()
        ).stream()
                .filter(document -> !additionalResultsContext.excludedSources().contains(
                        new AiSourceKey(document.sourceType(), document.sourceId())))
                .filter(document -> evidenceService.documentIdentityKeys(document).stream()
                        .noneMatch(additionalResultsContext.excludedIdentityKeys()::contains))
                .toList();
        if (additionalResultsContext.isFollowUp()) {
            retrievedDocuments = evidenceService.deduplicateInstitutions(retrievedDocuments);
        }

        log.info("[AI] 검색 문서 수: {}", retrievedDocuments.size());
        log.debug("[AI] 검색 documentKeys: {}",
                retrievedDocuments.stream()
                        .map(AiReferenceDocument::documentKey)
                        .toList());

        if (retrievedDocuments.isEmpty()) {
            log.info("[AI] 내부 문서 없음, 외부 검색 시작");
            return fallbackService.externalOrNoResult(
                    chatRoom,
                    userMessage,
                    searchQuestion,
                    searchQueries,
                    profile,
                    questionContext.searchScope()
            );
        }

        log.debug("[AI] 답변 생성 시작");

        GeneratedAiAnswer generated = answerGenerator.generate(
                resolvedContent, profile, retrievedDocuments
        );
        generated = normalizeListedResultCount(
                generated,
                questionContext.requestedResultCount(),
                additionalResultsContext.isFollowUp()
        );

        log.debug("[AI] 답변 생성 완료");
        log.debug("[AI] citedDocumentKeys: {}", generated.citedDocumentKeys());

        if ((siteListAnswerValidator.requiresValidation(content)
                || siteListAnswerValidator.requiresValidation(resolvedContent))
                && !siteListAnswerValidator.isValid(generated, retrievedDocuments)) {
            log.warn("[AI] 사이트 목록 항목과 인용 근거 검증 실패, 외부 검색 시작");
            return fallbackService.externalOrNoResult(
                    chatRoom,
                    userMessage,
                    searchQuestion,
                    searchQueries,
                    profile,
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
                    profile,
                    questionContext.searchScope()
            );
        }

        boolean warning = evidenceService.hasIncorrectFeedback(citedSources);
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, generated.answer(), warning,
                AiAnswerStatus.ANSWERED, citedSources);

        return fallbackService.sourceBacked(message, citedSources, AiAnswerStatus.ANSWERED);
    }

    private AiQuestionContext resolveQuestionContext(
            Long chatRoomId,
            String content,
            AiUserProfile profile,
            AiConversationContext conversationContext
    ) {
        Optional<Region> explicitRegion = resolveExplicitRehabRegion(content);
        if (explicitRegion.isPresent()) {
            return localRehabContext(profile, explicitRegion.get());
        }

        Optional<AiStarterQuestionType> questionType =
                AiStarterQuestionType.fromQuestion(content);
        if (questionType.isPresent()) {
            return starterQuestionContext(profile, questionType.get());
        }

        Optional<Region> followUpRegion = resolveRegionFollowUp(chatRoomId, content);
        if (followUpRegion.isPresent()) {
            return localRehabContext(profile, followUpRegion.get());
        }

        return questionContextResolver.resolve(content, profile, conversationContext);
    }

    private GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults
    ) {
        int actualCount = generated.answerItems().size();
        if (actualCount == 0
                || requestedResultCount == null
                || actualCount >= requestedResultCount) {
            return generated;
        }
        String answer = generated.answer()
                .replaceAll(
                        "(?m)\\s*현재 확인 가능한 관련 (?:항목|학교|기관|사이트)은? "
                                + "\\d+개입니다\\.\\s*",
                        "\n"
                )
                .replaceAll(
                        "(?m)\\s*이전에 안내한 항목을 제외하면,? 추가로 확인 가능한 "
                                + "관련 항목은 \\d+개입니다\\.\\s*",
                        "\n"
                )
                .trim();
        String countMessage = additionalResults
                ? "이전에 안내한 항목을 제외하면, 추가로 확인 가능한 관련 항목은 "
                        + actualCount + "개입니다."
                : "현재 확인 가능한 관련 항목은 " + actualCount + "개입니다.";
        answer = answer + "\n\n" + countMessage;
        return new GeneratedAiAnswer(
                answer,
                generated.citedDocumentKeys(),
                generated.answerItems()
        );
    }

    private boolean isSelfContainedResourceQuestion(
            String originalQuestion,
            String resolvedQuestion,
            AiQuestionRegionResolver.RegionResolution regionResolution
    ) {
        return questionContextResolver.isSelfContainedResourceQuestion(
                originalQuestion, resolvedQuestion, regionResolution);
    }

    private boolean isLocalResourceTarget(String question) {
        return questionContextResolver.isLocalResourceTarget(question);
    }

    private boolean shouldRouteStarterQuestion(AiQuestionContext context) {
        return context.questionType().orElse(null) != AiStarterQuestionType.WELFARE_SITES
                || context.searchScope() != AiSearchScope.LOCAL_RESOURCE;
    }

    private AiQuestionContext starterQuestionContext(
            AiUserProfile profile,
            AiStarterQuestionType questionType
    ) {
        return new AiQuestionContext(
                profile,
                Optional.of(questionType),
                Optional.empty(),
                searchScope(questionType),
                List.of(),
                null,
                null,
                false,
                null,
                List.of(),
                false,
                null,
                null
        );
    }

    private AiSearchScope searchScope(AiStarterQuestionType questionType) {
        return switch (questionType) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_RESOURCE;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION ->
                    AiSearchScope.NATIONAL_POLICY;
            default -> AiSearchScope.GENERAL;
        };
    }

    private AiQuestionContext localRehabContext(
            AiUserProfile profile,
            Region region
    ) {
        AiUserProfile regionalProfile = profile.withRegion(
                region.getFullName(),
                region.getRegionLevel1(),
                region.getRegionLevel2()
        );
        return starterQuestionContext(
                regionalProfile,
                AiStarterQuestionType.LOCAL_REHAB_CENTERS
        );
    }

    private Optional<Region> resolveExplicitRehabRegion(String content) {
        String normalizedQuestion = normalizeQuestion(content);
        boolean rehabRecommendation = normalizedQuestion.contains("재활센터")
                && (normalizedQuestion.contains("추천")
                || normalizedQuestion.contains("알려"));
        if (!rehabRecommendation) {
            return Optional.empty();
        }

        String normalizedContent = normalizeSpacing(content);
        return regionRepository.findMentionedInQuestion(
                        normalizedContent,
                        PageRequest.of(0, 1)
                )
                .stream()
                .filter(region -> isGenericRehabQuestion(
                        normalizedContent,
                        region
                ))
                .findFirst();
    }

    private boolean isGenericRehabQuestion(
            String question,
            Region region
    ) {
        String questionWithoutRegion = question
                .replace(region.getFullName(), " ")
                .trim()
                .replaceFirst("^(에서|의|에|내)\\s*", "");
        String normalizedQuestion = normalizeQuestion(questionWithoutRegion)
                .replaceFirst("추천해주세요$", "추천해줘")
                .replaceFirst("알려주세요$", "알려줘");
        return EXPLICIT_REGION_REHAB_QUESTIONS.contains(normalizedQuestion);
    }

    private Optional<Region> resolveRegionFollowUp(
            Long chatRoomId,
            String content
    ) {
        boolean awaitingRegion = aiMessageRepository
                .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId,
                        SenderType.AI
                )
                .filter(message -> message.getContent() == null
                        || !message.getContent().startsWith(
                        AMBIGUOUS_REGION_MESSAGE_PREFIX))
                .map(AiMessage::getAiAnswerStatus)
                .filter(status -> status == AiAnswerStatus.REGION_REQUIRED)
                .isPresent();
        if (!awaitingRegion) {
            return Optional.empty();
        }

        String regionName = normalizeSpacing(content)
                .replaceFirst("(입니다|이에요|예요|이야|야)$", "")
                .trim();
        if (regionName.isEmpty()) {
            return Optional.empty();
        }
        return regionRepository.findByFullName(regionName);
    }

    private String normalizeQuestion(String content) {
        return normalizeSpacing(content).replaceAll("\\s+", "");
    }

    private String normalizeSpacing(String content) {
        return content == null
                ? ""
                : content.trim()
                        .replaceFirst("[.!?~]+$", "")
                        .trim()
                        .replaceAll("\\s+", " ");
    }

    private List<AiReferenceDocument> retrieveDocuments(
            String originalQuestion,
            List<String> expandedQueries,
            String searchGoal,
            List<AiRequiredConcept> requiredConcepts,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        return documentSearchService.retrieve(
                originalQuestion,
                expandedQueries,
                searchGoal,
                requiredConcepts,
                profile,
                searchScope
        );
    }

    private AiUserProfile toProfile(
            User user,
            User disabilityProfileUser,
            AiScrapInterests scrapInterests
    ) {
        Region region = user.getRegion();
        return new AiUserProfile(
                region == null ? null : region.getFullName(),
                region == null ? null : region.getRegionLevel1(),
                region == null ? null : region.getRegionLevel2(),
                user.getChildAge(),
                disabilityProfileUser.getDisabilityTypes().stream()
                        .map(Enum::name)
                        .toList(),
                user.getInterestCategories().stream()
                        .map(Enum::name)
                        .toList(),
                user.getKeywordText(),
                scrapInterests.infoTitles(),
                scrapInterests.newsTitles(),
                scrapInterests.communityTopics()
        );
    }

    private AiScrapInterests loadScrapInterestsSafely(Long userId) {
        try {
            return scrapInterestService.findRecentInterests(userId);
        } catch (Exception e) {
            log.warn("[AI] 최근 스크랩 관심 정보 조회 실패, 기본 프로필로 처리합니다.", e);
            return AiScrapInterests.empty();
        }
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
