package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.*;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.URI;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
            "(?<!\\d)(?:0\\d{1,2})[- .]?\\d{3,4}[- .]?\\d{4}(?!\\d)");
    private static final Pattern LOCAL_RESOURCE_PATTERN = Pattern.compile(
            "(학교|센터|기관|병원|의원|약국|복지관|시설|교육원|상담소|지원사업|지원서비스)");
    private static final Pattern RELATIVE_LOCAL_REGION_PATTERN = Pattern.compile(
            "(우리\\s*(지역|동네)|근처|주변)");
    private static final Pattern ADDITIONAL_RESULTS_PATTERN = Pattern.compile(
            "(?:"
                    + "(?:\\d+(?:개|곳))?더(?:\\d+(?:개|곳))?"
                    + "|더많은(?:곳|기관|학교|센터|서비스|제도|항목)?"
                    + "|추가로?(?:\\d+(?:개|곳))?"
                    + "|다른(?:곳|기관|학교|센터|서비스|제도|항목)"
                    + ")(?:알려줘|알려주세요|추천해줘|추천해주세요)$");
    private static final String AMBIGUOUS_REGION_MESSAGE_PREFIX =
            "확인할 지역이 여러 곳입니다. ";
    private static final String NO_RESULT_MESSAGE = "관련 정보를 찾을 수 없습니다.";
    private static final Set<String> EXPLICIT_REGION_REHAB_QUESTIONS = Set.of(
            "재활센터추천해줘",
            "재활센터를추천해줘",
            "재활센터알려줘",
            "재활센터를알려줘"
    );
    private static final Set<InfoSubCategory> AI_SEARCHABLE_INFO_SUB_CATEGORIES = Set.of(
            InfoSubCategory.PRIMARY_CARE,
            InfoSubCategory.EMERGENCY_CLINIC,
            InfoSubCategory.THERAPY_REHAB,
            InfoSubCategory.WELFARE_CENTER,
            InfoSubCategory.FAMILY_SUPPORT,
            InfoSubCategory.PRIVATE_WELFARE,
            InfoSubCategory.NATIONAL_WELFARE,
            InfoSubCategory.LOCAL_WELFARE,
            InfoSubCategory.SPECIAL_SCHOOL,
            InfoSubCategory.SPECIAL_EDU_SUPPORT,
            InfoSubCategory.LIFELONG_EDU,
            InfoSubCategory.REALTIME_JOB,
            InfoSubCategory.STANDARD_WORKPLACE
    );
    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final UserRepository userRepository;
    private final RegionRepository regionRepository;
    private final AiDocumentRetriever documentRetriever;
    private final AiAnswerGenerator answerGenerator;
    private final AiExternalAnswerProvider externalAnswerProvider;
    private final AiMessagePersistenceService persistenceService;
    private final AiMessageFailureService failureService;
    private final AiSourceReviewRepository aiSourceReviewRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiRequestGuard requestGuard;
    private final AiReferenceDocumentResolver referenceDocumentResolver;
    private final AiStarterQuestionRouter starterQuestionRouter;
    private final AiQuestionIntentClassifier questionIntentClassifier;
    private final AiScrapInterestService scrapInterestService;
    private final AiQuestionRegionResolver questionRegionResolver;

    @Value("${bodeum.ai.conversation.recent-turn-count:3}")
    private int recentConversationTurnCount = 3;

    @Value("${bodeum.ai.result.max-count:10}")
    private int maxResultCount = 10;

    @Value("${bodeum.ai.rag.max-supplemental-concept-searches:3}")
    private int maxSupplementalConceptSearches = 3;

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

        AdditionalResultsContext additionalResultsContext =
                resolveAdditionalResultsContext(chatRoom.getId(), content);
        ConversationContext conversationContext =
                resolveConversationContext(chatRoom.getId());
        AiUserProfile baseProfile = toProfile(
                user, userWithDisabilities, scrapInterests);
        if (RELATIVE_LOCAL_REGION_PATTERN.matcher(content).find()
                && (baseProfile.region() == null || baseProfile.region().isBlank())) {
            persistenceService.updateUserMessageContext(
                    userMessage.getId(), content, null, userMessage.getId());
            return createRegionRequiredResponse(
                    chatRoom,
                    userMessage,
                    "어느 지역을 기준으로 찾을까요? 시·도와 시·군·구를 알려주세요."
            );
        }
        AiQuestionRegionResolver.RegionResolution regionResolution =
                questionRegionResolver.resolve(content, baseProfile);
        if (LOCAL_RESOURCE_PATTERN.matcher(content).find()
                && regionResolution.isAmbiguous()) {
            persistenceService.updateUserMessageContext(
                    userMessage.getId(), content, null, userMessage.getId());
            return createRegionRequiredResponse(
                    chatRoom, userMessage, regionResolution.ambiguityMessage());
        }

        QuestionContext questionContext = resolveQuestionContext(
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
                followUp ? conversationContext.parentUserMessageId() : null,
                !followUp || conversationContext.rootUserMessageId() == null
                        ? userMessage.getId()
                        : conversationContext.rootUserMessageId()
        );
        if (questionContext.safetyGuidance().isPresent()) {
            log.info("[AI] 안전 응답 안내로 전환");
            return createNoEvidenceResponse(
                    chatRoom,
                    userMessage,
                    questionContext.safetyGuidance().get()
            );
        }
        if (questionContext.needsClarification()) {
            log.info("[AI] 사용자 확인 질문으로 전환");
            return createClarificationRequiredResponse(
                    chatRoom,
                    userMessage,
                    questionContext.clarificationQuestion()
            );
        }

        AiUserProfile profile = questionContext.profile();
        Optional<AiStarterQuestionAnswer> starterAnswer =
                questionContext.requestedResultCount() == null
                        ? questionContext.questionType()
                                .flatMap(type -> starterQuestionRouter.route(type, profile))
                        : Optional.empty();
        if (starterAnswer.isPresent()) {
            AiStarterQuestionAnswer answer = starterAnswer.get();
            if (answer.isRegionRequired() || answer.hasEvidence()) {
                return saveStarterAnswer(chatRoom, userMessage, answer);
            }
            log.info("[AI] 추천 질문 출처 없음, 일반 질문 흐름으로 전환");
        }

        String searchQuestion = appendRequestedResultCount(contextualizeLocalRegion(
                resolvedContent,
                profile,
                questionContext.searchScope()
        ), questionContext.requestedResultCount());
        searchQuestion = appendAdditionalResultsSearchContext(
                searchQuestion, additionalResultsContext);
        List<String> searchQueries = ensureBroaderDisabilityTargetQuery(
                searchQuestion,
                contextualizeLocalRegions(
                        questionContext.retrievalQueries(),
                        profile,
                        questionContext.searchScope()
                ).stream()
                        .map(query -> appendRequestedResultCount(
                                query,
                                questionContext.requestedResultCount()
                        ))
                        .map(query -> appendAdditionalResultsSearchContext(
                                query,
                                additionalResultsContext
                        ))
                        .toList(),
                profile,
                questionContext.searchScope()
        );

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
                .filter(document -> documentIdentityKeys(document).stream()
                        .noneMatch(additionalResultsContext.excludedIdentityKeys()::contains))
                .toList();
        if (additionalResultsContext.isFollowUp()) {
            retrievedDocuments = deduplicateInstitutions(retrievedDocuments);
        }

        log.info("[AI] 검색 문서 수: {}", retrievedDocuments.size());
        log.debug("[AI] 검색 documentKeys: {}",
                retrievedDocuments.stream()
                        .map(AiReferenceDocument::documentKey)
                        .toList());

        if (retrievedDocuments.isEmpty()) {
            log.info("[AI] 내부 문서 없음, 외부 검색 시작");
            return createExternalOrNoResultResponse(
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

        log.debug("[AI] 답변 생성 완료");
        log.debug("[AI] citedDocumentKeys: {}", generated.citedDocumentKeys());

        List<AiReferenceDocument> citedSources =
                validateCitations(generated, retrievedDocuments);

        // 검색 결과가 있더라도 LLM이 그 문서를 실제 근거로 인용하지 않았다면,
        // 내부 답변을 폐기하고 등록된 외부 사이트 범위에서 근거를 다시 찾는다.
        if (citedSources.isEmpty()) {
            log.info("[AI] 내부 문서 인용 근거 없음, 외부 검색 시작");
            return createExternalOrNoResultResponse(
                    chatRoom,
                    userMessage,
                    searchQuestion,
                    searchQueries,
                    profile,
                    questionContext.searchScope()
            );
        }

        boolean warning = hasIncorrectFeedback(citedSources);
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, generated.answer(), warning,
                AiAnswerStatus.ANSWERED, citedSources);

        return sourceBackedResponse(
                message, citedSources, warningResponse(warning), AiAnswerStatus.ANSWERED);
    }

    private QuestionContext resolveQuestionContext(
            Long chatRoomId,
            String content,
            AiUserProfile profile,
            ConversationContext conversationContext
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

        AiQuestionAnalysis analysis = conversationContext.hasContext()
                ? questionIntentClassifier.analyze(
                        content,
                        conversationContext.previousUserQuestion(),
                        conversationContext.previousAiAnswer()
                )
                : questionIntentClassifier.analyze(content);
        AiQuestionIntent intent = analysis.intent();
        String resolvedQuestion = analysis.resolvedQuestion() == null
                ? content
                : analysis.resolvedQuestion();
        AiSearchScope searchScope = resolveSearchScope(intent, analysis.searchScope());
        AiQuestionRegionResolver.RegionResolution regionResolution =
                questionRegionResolver.resolve(resolvedQuestion, profile);
        if (intent == AiQuestionIntent.NONE
                && !regionResolution.isNotFound()
                && LOCAL_RESOURCE_PATTERN.matcher(resolvedQuestion).find()) {
            searchScope = AiSearchScope.LOCAL_RESOURCE;
        }
        InfoSubCategory infoSubCategory = resolveInfoSubCategory(
                resolvedQuestion,
                analysis.infoSubCategory()
        );
        AiUserProfile searchProfile = (regionResolution.isResolved()
                        ? regionResolution.applyTo(profile)
                        : profile)
                .withInfoSubCategory(infoSubCategory);
        return new QuestionContext(
                searchProfile,
                intent.starterQuestionType(),
                intent.safetyGuidance(),
                searchScope,
                intent == AiQuestionIntent.NONE
                        ? analysis.retrievalQueries()
                        : List.of(),
                analysis.requestedResultCount(),
                resolvedQuestion,
                analysis.followUp(),
                analysis.searchGoal(),
                analysis.requiredConcepts(),
                analysis.needsClarification(),
                analysis.clarificationQuestion()
        );
    }

    private QuestionContext starterQuestionContext(
            AiUserProfile profile,
            AiStarterQuestionType questionType
    ) {
        return new QuestionContext(
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
                null
        );
    }

    private AiSearchScope resolveSearchScope(
            AiQuestionIntent intent,
            AiSearchScope analyzedSearchScope
    ) {
        return switch (intent) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_RESOURCE;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION ->
                    AiSearchScope.NATIONAL_POLICY;
            default -> analyzedSearchScope == null
                    ? AiSearchScope.GENERAL
                    : analyzedSearchScope;
        };
    }

    private InfoSubCategory resolveInfoSubCategory(
            String question,
            InfoSubCategory analyzedCategory
    ) {
        String normalizedQuestion = normalizeQuestion(question);
        if (normalizedQuestion.contains("특수교육지원센터")) {
            return InfoSubCategory.SPECIAL_EDU_SUPPORT;
        }
        if (normalizedQuestion.contains("특수학교")) {
            return InfoSubCategory.SPECIAL_SCHOOL;
        }
        if (normalizedQuestion.contains("장애인평생교육")) {
            return InfoSubCategory.LIFELONG_EDU;
        }
        if (normalizedQuestion.contains("응급의료기관")) {
            return InfoSubCategory.EMERGENCY_CLINIC;
        }
        if (normalizedQuestion.contains("치료재활기관")
                || normalizedQuestion.contains("재활센터")) {
            return InfoSubCategory.THERAPY_REHAB;
        }
        if (normalizedQuestion.contains("장애인복지관")) {
            return InfoSubCategory.WELFARE_CENTER;
        }
        if (normalizedQuestion.contains("장애인가족지원센터")) {
            return InfoSubCategory.FAMILY_SUPPORT;
        }
        if (normalizedQuestion.contains("장애인표준사업장")) {
            return InfoSubCategory.STANDARD_WORKPLACE;
        }
        return analyzedCategory != null
                && AI_SEARCHABLE_INFO_SUB_CATEGORIES.contains(analyzedCategory)
                ? analyzedCategory
                : null;
    }

    private AiSearchScope searchScope(AiStarterQuestionType questionType) {
        return switch (questionType) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_RESOURCE;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION ->
                    AiSearchScope.NATIONAL_POLICY;
            default -> AiSearchScope.GENERAL;
        };
    }

    private QuestionContext localRehabContext(
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

    private String appendRequestedResultCount(
            String question,
            Integer requestedResultCount
    ) {
        if (requestedResultCount == null || requestedResultCount <= 0) {
            return question;
        }
        int searchResultCount = Math.min(requestedResultCount, maxResultCount);
        return question + "\n요청 결과 개수: " + searchResultCount + "개";
    }

    private AdditionalResultsContext resolveAdditionalResultsContext(
            Long chatRoomId,
            String content
    ) {
        if (!isAdditionalResultsQuestion(content)) {
            return AdditionalResultsContext.empty();
        }

        List<AiMessage> nearestUserMessages = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId,
                        SenderType.USER,
                        PageRequest.of(0, 2)
                );
        if (nearestUserMessages.size() >= 2) {
            AiMessage previousUserMessage = nearestUserMessages.get(1);
            Long rootMessageId = previousUserMessage.getContextRootMessageId();
            if (rootMessageId != null && rootMessageId > 0) {
                Optional<AiMessage> rootMessage = aiMessageRepository.findById(rootMessageId);
                List<AiMessage> previousAiMessages = aiMessageRepository
                        .findByChatRoomIdAndContextRootMessageIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                                chatRoomId,
                                rootMessageId,
                                SenderType.AI
                        );
                if (rootMessage.isPresent() && !previousAiMessages.isEmpty()) {
                    return additionalResultsContext(
                            rootMessage.get(),
                            previousAiMessages
                    );
                }
            }
        }

        return AdditionalResultsContext.empty();
    }

    private AdditionalResultsContext additionalResultsContext(
            AiMessage baseQuestion,
            List<AiMessage> previousAiMessages
    ) {
        List<AiResponseSourceProjection> previousSources = aiResponseSourceRepository
                .findAllByMessageIds(previousAiMessages.stream()
                        .map(AiMessage::getId)
                        .toList());
        if (previousSources.isEmpty()) {
            return AdditionalResultsContext.empty();
        }

        Set<AiSourceKey> excludedSources = previousSources.stream()
                .map(source -> new AiSourceKey(source.getSourceType(), source.getSourceId()))
                .collect(java.util.stream.Collectors.toSet());
        List<String> excludedTitles = previousSources.stream()
                .map(AiResponseSourceProjection::getSourceTitle)
                .filter(title -> title != null && !title.isBlank())
                .distinct()
                .toList();
        Set<String> excludedIdentityKeys = previousSources.stream()
                .flatMap(source -> sourceIdentityKeys(
                        source.getSourceTitle(),
                        source.getSourceUrl()
                ).stream())
                .collect(java.util.stream.Collectors.toSet());
        return new AdditionalResultsContext(
                baseQuestion.getResolvedQuestion() == null
                        || baseQuestion.getResolvedQuestion().isBlank()
                        ? baseQuestion.getContent()
                        : baseQuestion.getResolvedQuestion(),
                excludedSources,
                excludedTitles,
                excludedIdentityKeys
        );
    }

    private List<AiReferenceDocument> deduplicateInstitutions(
            List<AiReferenceDocument> documents
    ) {
        Set<String> seenIdentityKeys = new HashSet<>();
        List<AiReferenceDocument> distinctDocuments = new ArrayList<>();
        for (AiReferenceDocument document : documents) {
            Set<String> identityKeys = documentIdentityKeys(document);
            if (!identityKeys.isEmpty()
                    && identityKeys.stream().anyMatch(seenIdentityKeys::contains)) {
                continue;
            }
            distinctDocuments.add(document);
            seenIdentityKeys.addAll(identityKeys);
        }
        return List.copyOf(distinctDocuments);
    }

    private Set<String> documentIdentityKeys(AiReferenceDocument document) {
        Set<String> identityKeys = new HashSet<>(
                sourceIdentityKeys(document.title(), document.url()));
        if (!identityKeys.isEmpty()) {
            return identityKeys;
        }
        Matcher phoneMatcher = PHONE_NUMBER_PATTERN.matcher(
                document.content() == null ? "" : document.content());
        while (phoneMatcher.find()) {
            identityKeys.add("phone:" + phoneMatcher.group().replaceAll("\\D", ""));
        }
        return identityKeys;
    }

    private Set<String> sourceIdentityKeys(String title, String url) {
        Set<String> identityKeys = new HashSet<>();
        String normalizedTitle = normalizeInstitutionTitle(title);
        if (!normalizedTitle.isBlank()) {
            identityKeys.add("title:" + normalizedTitle);
        }
        String normalizedUrl = normalizeInstitutionUrl(url);
        if (!normalizedUrl.isBlank()) {
            identityKeys.add("url:" + normalizedUrl);
        }
        return identityKeys;
    }

    private String normalizeInstitutionTitle(String title) {
        if (title == null) {
            return "";
        }
        return title.toLowerCase(Locale.ROOT)
                .replaceAll("^\\s*\\[[^]]+]\\s*", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String normalizeInstitutionUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            String absoluteUrl = url.contains("://") ? url : "https://" + url;
            URI uri = URI.create(absoluteUrl.trim()).normalize();
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return "";
            }
            String path = uri.getPath() == null ? "" : uri.getPath();
            path = path.replaceAll("/+$", "");
            return host.toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "") + path;
        } catch (IllegalArgumentException ignored) {
            return url.trim().toLowerCase(Locale.ROOT).replaceAll("/+$", "");
        }
    }

    private ConversationContext resolveConversationContext(Long chatRoomId) {
        int resolvedTurnCount = Math.max(1, recentConversationTurnCount);
        List<AiMessage> recentUserMessages = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId,
                        SenderType.USER,
                        PageRequest.of(0, resolvedTurnCount + 1)
                );
        if (recentUserMessages.size() < 2) {
            return ConversationContext.empty();
        }
        List<AiMessage> previousAiMessages = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId,
                        SenderType.AI,
                        PageRequest.of(0, resolvedTurnCount)
                );
        if (previousAiMessages.isEmpty()) {
            previousAiMessages = aiMessageRepository
                    .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                            chatRoomId,
                            SenderType.AI
                    )
                    .map(List::of)
                    .orElseGet(List::of);
        }
        if (previousAiMessages.isEmpty()) {
            return ConversationContext.empty();
        }
        List<AiMessage> previousUserMessages = recentUserMessages.subList(
                1,
                Math.min(
                        recentUserMessages.size(),
                        resolvedTurnCount + 1
                )
        );
        return new ConversationContext(
                formatPreviousUserQuestions(previousUserMessages),
                formatPreviousAiAnswers(previousAiMessages),
                previousUserMessages.getFirst().getId(),
                previousUserMessages.getFirst().getContextRootMessageId() == null
                        || previousUserMessages.getFirst().getContextRootMessageId() <= 0
                        ? previousUserMessages.getFirst().getId()
                        : previousUserMessages.getFirst().getContextRootMessageId()
        );
    }

    private String formatPreviousUserQuestions(List<AiMessage> messages) {
        if (messages.size() == 1) {
            AiMessage message = messages.getFirst();
            return message.getResolvedQuestion() == null
                    || message.getResolvedQuestion().isBlank()
                    ? message.getContent()
                    : message.getResolvedQuestion();
        }
        StringBuilder context = new StringBuilder();
        for (int index = messages.size() - 1; index >= 0; index--) {
            AiMessage message = messages.get(index);
            String question = message.getResolvedQuestion() == null
                    || message.getResolvedQuestion().isBlank()
                    ? message.getContent()
                    : message.getResolvedQuestion();
            if (question != null && !question.isBlank()) {
                context.append("- ").append(question).append('\n');
            }
        }
        return context.toString().trim();
    }

    private String formatPreviousAiAnswers(List<AiMessage> messages) {
        if (messages.size() == 1) {
            return messages.getFirst().getContent();
        }
        StringBuilder context = new StringBuilder();
        for (int index = messages.size() - 1; index >= 0; index--) {
            String answer = messages.get(index).getContent();
            if (answer != null && !answer.isBlank()) {
                context.append("- ").append(answer).append('\n');
            }
        }
        return context.toString().trim();
    }

    private boolean isAdditionalResultsQuestion(String content) {
        String normalized = normalizeQuestion(content);
        if (normalized.contains("자세히")
                || normalized.contains("상세히")
                || normalized.contains("내용")
                || normalized.contains("방법")) {
            return false;
        }
        return ADDITIONAL_RESULTS_PATTERN.matcher(normalized).find();
    }

    private String appendAdditionalResultsSearchContext(
            String question,
            AdditionalResultsContext context
    ) {
        if (!context.isFollowUp()) {
            return question;
        }
        StringBuilder contextualized = new StringBuilder(question)
                .append("\n검색 후보 개수: ").append(maxResultCount).append("개");
        if (!context.excludedTitles().isEmpty()) {
            contextualized.append("\n이전에 안내하여 제외할 기관: ")
                    .append(String.join(", ", context.excludedTitles()));
        }
        return contextualized.toString();
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
        List<String> queries = new ArrayList<>();
        queries.add(originalQuestion);
        if (expandedQueries != null) {
            queries.addAll(expandedQueries);
        }
        List<String> distinctQueries = queries.stream()
                .filter(query -> query != null && !query.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();

        List<List<AiReferenceDocument>> documentsByQuery = new ArrayList<>();
        for (int queryIndex = 0; queryIndex < distinctQueries.size(); queryIndex++) {
            String query = distinctQueries.get(queryIndex);
            List<AiReferenceDocument> queryDocuments =
                    documentRetriever.retrieve(query, profile, searchScope);
            documentsByQuery.add(queryDocuments);
            log.debug(
                    "[AI] 질의별 검색 결과: queryIndex={}, documentKeys={}",
                    queryIndex,
                    queryDocuments.stream()
                            .map(AiReferenceDocument::documentKey)
                            .toList()
            );
        }

        LinkedHashMap<String, AiReferenceDocument> documentsByKey = new LinkedHashMap<>();
        preserveRequiredConceptDocuments(
                    requiredConcepts,
                    searchGoal,
                    documentsByQuery,
                    documentsByKey,
                    profile,
                    searchScope
        );
        if (normalizeQuestion(originalQuestion).contains("장애아동")) {
            // 원문·전국·지역 결과 균형 병합
            mergeRoundRobin(documentsByQuery, documentsByKey);
        } else {
            // 기존 원문 우선 병합
            mergeOriginalFirst(documentsByQuery, documentsByKey);
        }

        List<AiReferenceDocument> merged = documentsByKey.values().stream()
                .limit(maxResultCount)
                .toList();
        log.info(
                "[AI] 다중 검색 완료: queryCount={}, uniqueDocumentCount={}",
                distinctQueries.size(),
                merged.size()
        );
        return referenceDocumentResolver.resolve(merged);
    }

    private void preserveRequiredConceptDocuments(
            List<AiRequiredConcept> requiredConcepts,
            String searchGoal,
            List<List<AiReferenceDocument>> documentsByQuery,
            LinkedHashMap<String, AiReferenceDocument> documentsByKey,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (requiredConcepts == null || requiredConcepts.isEmpty()) {
            return;
        }

        List<AiReferenceDocument> candidates = new ArrayList<>(documentsByQuery.stream()
                .flatMap(List::stream)
                .toList());
        int supplementalSearchCount = 0;
        for (int conceptIndex = 0; conceptIndex < requiredConcepts.size(); conceptIndex++) {
            AiRequiredConcept concept = requiredConcepts.get(conceptIndex);
            Optional<AiReferenceDocument> matched = findConceptDocument(
                    candidates,
                    concept,
                    profile,
                    documentsByKey.keySet()
            );
            if (matched.isEmpty()
                    && supplementalSearchCount < maxSupplementalConceptSearches) {
                supplementalSearchCount++;
                List<AiReferenceDocument> supplemented = documentRetriever.retrieve(
                        supplementalResultQuery(concept, searchGoal, profile),
                        profile,
                        searchScope
                );
                candidates.addAll(supplemented);
                matched = findConceptDocument(
                        supplemented,
                        concept,
                        profile,
                        documentsByKey.keySet()
                );
            }
            matched.ifPresent(document -> documentsByKey.putIfAbsent(
                    document.documentKey(), document));
            if (matched.isEmpty()) {
                log.warn("[AI] 필수 검색 개념 근거 문서 누락: conceptIndex={}", conceptIndex);
            }
        }
    }

    private String supplementalResultQuery(
            AiRequiredConcept concept,
            String searchGoal,
            AiUserProfile profile
    ) {
        StringBuilder query = new StringBuilder();
        if (concept.requiresUserRegion()
                && profile != null
                && profile.region() != null
                && !profile.region().isBlank()) {
            query.append(profile.region()).append(' ');
        }
        query.append(concept.retrievalQuery());
        if (searchGoal != null && !searchGoal.isBlank()) {
            query.append(' ').append(searchGoal.trim());
        }
        return query.append("\n요청 결과 개수: ")
                .append(maxResultCount)
                .append("개")
                .toString();
    }

    private Optional<AiReferenceDocument> findConceptDocument(
            List<AiReferenceDocument> documents,
            AiRequiredConcept concept,
            AiUserProfile profile,
            Set<String> excludedDocumentKeys
    ) {
        List<String> matchTerms = concept.matchTerms().stream()
                .map(this::normalizeQuestion)
                .toList();
        List<String> excludeTerms = concept.excludeTerms().stream()
                .map(this::normalizeQuestion)
                .toList();
        return documents.stream()
                .filter(document -> !excludedDocumentKeys.contains(document.documentKey()))
                .filter(document -> {
                    String searchableText = normalizeQuestion(document.title())
                            + normalizeQuestion(document.content());
                    return matchTerms.stream().allMatch(searchableText::contains)
                            && excludeTerms.stream().noneMatch(searchableText::contains)
                            && matchesRequiredRegion(searchableText, concept, profile);
                })
                .findFirst();
    }

    private boolean matchesRequiredRegion(
            String searchableText,
            AiRequiredConcept concept,
            AiUserProfile profile
    ) {
        if (!concept.requiresUserRegion()) {
            return true;
        }
        if (profile == null) {
            return false;
        }
        String region = normalizeQuestion(profile.region());
        String regionLevel2 = normalizeQuestion(profile.regionLevel2());
        return (!region.isBlank() && searchableText.contains(region))
                || (!regionLevel2.isBlank() && searchableText.contains(regionLevel2));
    }

    private void mergeRoundRobin(
            List<List<AiReferenceDocument>> documentsByQuery,
            LinkedHashMap<String, AiReferenceDocument> documentsByKey
    ) {
        int maxRank = documentsByQuery.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        for (int rank = 0;
             rank < maxRank && documentsByKey.size() < maxResultCount;
             rank++) {
            for (List<AiReferenceDocument> queryDocuments : documentsByQuery) {
                if (rank < queryDocuments.size()) {
                    AiReferenceDocument document = queryDocuments.get(rank);
                    documentsByKey.putIfAbsent(document.documentKey(), document);
                }
                if (documentsByKey.size() >= maxResultCount) {
                    break;
                }
            }
        }
    }

    private void mergeOriginalFirst(
            List<List<AiReferenceDocument>> documentsByQuery,
            LinkedHashMap<String, AiReferenceDocument> documentsByKey
    ) {
        if (!documentsByQuery.isEmpty()) {
            int reservedExpandedSlots = Math.min(
                    documentsByQuery.size() - 1,
                    maxResultCount
            );
            int originalLimit = maxResultCount - reservedExpandedSlots;
            for (AiReferenceDocument document : documentsByQuery.getFirst()) {
                documentsByKey.putIfAbsent(document.documentKey(), document);
                if (documentsByKey.size() >= originalLimit) {
                    break;
                }
            }
        }

        for (int queryIndex = 1;
             queryIndex < documentsByQuery.size()
                     && documentsByKey.size() < maxResultCount;
             queryIndex++) {
            List<AiReferenceDocument> queryDocuments = documentsByQuery.get(queryIndex);
            if (!queryDocuments.isEmpty()) {
                AiReferenceDocument document = queryDocuments.getFirst();
                documentsByKey.putIfAbsent(document.documentKey(), document);
            }
        }

        int maxExpandedRank = documentsByQuery.stream()
                .skip(1)
                .mapToInt(List::size)
                .max()
                .orElse(0);
        for (int rank = 1;
             rank < maxExpandedRank
                     && documentsByKey.size() < maxResultCount;
             rank++) {
            for (int queryIndex = 1;
                 queryIndex < documentsByQuery.size();
                 queryIndex++) {
                List<AiReferenceDocument> queryDocuments =
                        documentsByQuery.get(queryIndex);
                if (rank < queryDocuments.size()) {
                    AiReferenceDocument document = queryDocuments.get(rank);
                    documentsByKey.putIfAbsent(document.documentKey(), document);
                }
                if (documentsByKey.size() >= maxResultCount) {
                    break;
                }
            }
        }

        if (!documentsByQuery.isEmpty()) {
            for (AiReferenceDocument document : documentsByQuery.getFirst()) {
                documentsByKey.putIfAbsent(document.documentKey(), document);
                if (documentsByKey.size() >= maxResultCount) {
                    break;
                }
            }
        }
    }

    private List<String> contextualizeLocalRegions(
            List<String> queries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (queries == null) {
            return List.of();
        }
        return queries.stream()
                .map(query -> contextualizeLocalRegion(query, profile, searchScope))
                .toList();
    }

    private List<String> ensureBroaderDisabilityTargetQuery(
            String question,
            List<String> queries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        List<String> expanded = new ArrayList<>();
        if (question != null) {
            String broaderTargetQuery = question.replaceAll("장애\\s*아동", "장애인");
            if (!broaderTargetQuery.equals(question)) {
                expanded.add(broaderTargetQuery);
                if (searchScope == AiSearchScope.NATIONAL_POLICY
                        && question.replaceAll("\\s+", "").contains("활동지원")
                        && profile != null
                        && profile.region() != null
                        && !profile.region().isBlank()) {
                    expanded.add(profile.region() + " " + broaderTargetQuery);
                }
            }
        }
        if (queries != null) {
            expanded.addAll(queries);
        }
        return expanded.stream()
                .filter(query -> query != null && !query.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();
    }

    private String contextualizeLocalRegion(
            String query,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (searchScope != AiSearchScope.LOCAL_RESOURCE
                || profile == null
                || profile.region() == null
                || profile.region().isBlank()) {
            return query;
        }
        return query
                .replace("우리 지역", profile.region())
                .replace("우리 동네", profile.region())
                .replace("근처", profile.region())
                .replace("주변", profile.region());
    }

    private CreateAiMessageResponse saveStarterAnswer(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            AiStarterQuestionAnswer answer
    ) {
        if (answer.isRegionRequired()) {
            return createRegionRequiredResponse(chatRoom, userMessage, answer.content());
        }
        if (!answer.hasEvidence()) {
            return createNoEvidenceResponse(chatRoom, userMessage);
        }

        boolean warning = hasIncorrectFeedback(answer.sources());
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(),
                chatRoom,
                answer.content(),
                warning,
                AiAnswerStatus.ANSWERED,
                answer.sources()
        );
        return sourceBackedResponse(
                message,
                answer.sources(),
                warningResponse(warning),
                AiAnswerStatus.ANSWERED
        );
    }

    private CreateAiMessageResponse createExternalOrNoResultResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        ExternalAiAnswer externalAnswer = externalAnswerProvider.search(
                question,
                retrievalQueries,
                profile,
                searchScope
        );
        if (!externalAnswer.hasEvidence()) {
            return createNoEvidenceResponse(chatRoom, userMessage);
        }

        boolean warning = hasIncorrectFeedback(externalAnswer.sources());
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, externalAnswer.answer(), warning,
                externalAnswer.answerStatus(), externalAnswer.sources());
        return sourceBackedResponse(
                message,
                externalAnswer.sources(),
                warningResponse(warning),
                externalAnswer.answerStatus()
        );
    }

    private CreateAiMessageResponse createRegionRequiredResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(),
                chatRoom,
                content,
                false,
                AiAnswerStatus.REGION_REQUIRED,
                List.of()
        );
        return new CreateAiMessageResponse(AiMessageResponse.regionRequired(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        ));
    }

    private CreateAiMessageResponse createNoEvidenceResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage
    ) {
        return createNoEvidenceResponse(chatRoom, userMessage, NO_RESULT_MESSAGE);
    }

    private CreateAiMessageResponse createClarificationRequiredResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(),
                chatRoom,
                content,
                false,
                AiAnswerStatus.CLARIFICATION_REQUIRED,
                List.of()
        );
        return new CreateAiMessageResponse(AiMessageResponse.clarificationRequired(
                message.getId(),
                message.getSenderType(),
                message.getContent(),
                message.getCreatedAt()
        ));
    }

    private CreateAiMessageResponse createNoEvidenceResponse(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String content
    ) {
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, content, false,
                AiAnswerStatus.NO_EVIDENCE, List.of());
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
        log.debug("[AI] citation 검증 시작");

        Set<String> citedKeys = new HashSet<>(
                generated.citedDocumentKeys() == null
                        ? List.of()
                        : generated.citedDocumentKeys()
        );

        // LLM이 임의의 출처를 만들어도 응답에 포함되지 않도록,
        // 실제 검색 결과에 존재하는 documentKey만 인용으로 인정한다.
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
        return aiSourceReviewRepository.existsWarningRequiredBySources(sourceKeys);
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
                ? AiMessageWarningResponse.incorrectSource()
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

    private record QuestionContext(
            AiUserProfile profile,
            Optional<AiStarterQuestionType> questionType,
            Optional<String> safetyGuidance,
            AiSearchScope searchScope,
            List<String> retrievalQueries,
            Integer requestedResultCount,
            String resolvedQuestion,
            boolean followUp,
            String searchGoal,
            List<AiRequiredConcept> requiredConcepts,
            boolean needsClarification,
            String clarificationQuestion
    ) {
    }

    private record ConversationContext(
            String previousUserQuestion,
            String previousAiAnswer,
            Long parentUserMessageId,
            Long rootUserMessageId
    ) {
        private static ConversationContext empty() {
            return new ConversationContext(null, null, null, null);
        }

        private boolean hasContext() {
            return previousUserQuestion != null
                    && !previousUserQuestion.isBlank()
                    && previousAiAnswer != null
                    && !previousAiAnswer.isBlank();
        }
    }

    private record AdditionalResultsContext(
            String previousQuestion,
            Set<AiSourceKey> excludedSources,
            List<String> excludedTitles,
            Set<String> excludedIdentityKeys
    ) {
        private static AdditionalResultsContext empty() {
            return new AdditionalResultsContext(
                    null, Set.of(), List.of(), Set.of());
        }

        private boolean isFollowUp() {
            return previousQuestion != null && !previousQuestion.isBlank();
        }
    }

}
