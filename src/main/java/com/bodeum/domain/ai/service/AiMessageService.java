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
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
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
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiMessageService {

    private static final int MAX_RETRIEVED_DOCUMENTS = 10;
    private static final Pattern REGION_LEVEL_2_PATTERN =
            Pattern.compile("([가-힣]+(?:시|군|구))");
    private static final Map<String, String> REGION_LEVEL_1_ALIASES = Map.of(
            "서울시", "서울특별시",
            "부산시", "부산광역시",
            "대구시", "대구광역시",
            "인천시", "인천광역시",
            "대전시", "대전광역시",
            "울산시", "울산광역시",
            "세종시", "세종특별자치시"
    );
    private static final String NO_RESULT_MESSAGE = "관련 정보를 찾을 수 없습니다.";
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
    private final AiDocumentRetriever documentRetriever;
    private final AiAnswerGenerator answerGenerator;
    private final AiExternalAnswerProvider externalAnswerProvider;
    private final AiMessagePersistenceService persistenceService;
    private final AiMessageFailureService failureService;
    private final AiSourceReviewRepository aiSourceReviewRepository;
    private final AiRequestGuard requestGuard;
    private final AiReferenceDocumentResolver referenceDocumentResolver;
    private final AiStarterQuestionRouter starterQuestionRouter;
    private final AiQuestionIntentClassifier questionIntentClassifier;
    private final AiScrapInterestService scrapInterestService;

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

        QuestionContext questionContext = resolveQuestionContext(
                chatRoom.getId(),
                content,
                toProfile(user, userWithDisabilities, scrapInterests)
        );
        if (questionContext.safetyGuidance().isPresent()) {
            log.info("[AI] 안전 응답 안내로 전환");
            return createNoEvidenceResponse(
                    chatRoom,
                    userMessage,
                    questionContext.safetyGuidance().get()
            );
        }

        AiUserProfile profile = questionContext.profile();
        Optional<AiStarterQuestionAnswer> starterAnswer =
                questionContext.questionType()
                        .flatMap(type -> starterQuestionRouter.route(type, profile));
        if (starterAnswer.isPresent()) {
            AiStarterQuestionAnswer answer = starterAnswer.get();
            if (answer.isRegionRequired() || answer.hasEvidence()) {
                return saveStarterAnswer(chatRoom, userMessage, answer);
            }
            log.info("[AI] 추천 질문 출처 없음, 일반 질문 흐름으로 전환");
        }

        log.debug("[AI] 문서 검색 시작");
        List<AiReferenceDocument> retrievedDocuments = retrieveDocuments(
                content,
                questionContext.retrievalQueries(),
                profile,
                questionContext.searchScope()
        );

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
                    content,
                    questionContext.retrievalQueries(),
                    profile,
                    questionContext.searchScope()
            );
        }

        log.debug("[AI] 답변 생성 시작");

        GeneratedAiAnswer generated = answerGenerator.generate(
                content, profile, retrievedDocuments
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
                    content,
                    questionContext.retrievalQueries(),
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
            AiUserProfile profile
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

        AiQuestionAnalysis analysis = questionIntentClassifier.analyze(content);
        AiQuestionIntent intent = analysis.intent();
        AiUserProfile searchProfile = applyExplicitSearchRegion(
                content,
                profile,
                analysis.searchScope()
        );
        return new QuestionContext(
                searchProfile,
                intent.starterQuestionType(),
                intent.safetyGuidance(),
                analysis.searchScope(),
                intent == AiQuestionIntent.NONE
                        ? analysis.retrievalQueries()
                        : List.of()
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
                List.of()
        );
    }

    private AiUserProfile applyExplicitSearchRegion(
            String question,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (searchScope != AiSearchScope.LOCAL_RESOURCE) {
            return profile;
        }

        Optional<Region> fullNameRegion = regionRepository.findMentionedInQuestion(
                        normalizeSpacing(question),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst();
        if (fullNameRegion.isPresent()) {
            return withRegion(profile, fullNameRegion.get());
        }

        String normalizedQuestion = normalizeQuestion(question);
        String explicitRegionLevel1 = null;
        for (Map.Entry<String, String> alias : REGION_LEVEL_1_ALIASES.entrySet()) {
            if (normalizedQuestion.contains(normalizeQuestion(alias.getKey()))) {
                Optional<Region> region = regionRepository
                        .findFirstByRegionLevel1OrderByIdAsc(alias.getValue());
                if (region.isPresent()) {
                    explicitRegionLevel1 = alias.getValue();
                    break;
                }
            }
        }
        String matchedRegionLevel1 = explicitRegionLevel1;

        Matcher matcher = REGION_LEVEL_2_PATTERN.matcher(normalizeSpacing(question));
        while (matcher.find()) {
            String regionLevel2 = matcher.group(1);
            List<Region> candidates = regionRepository
                    .findAllByRegionLevel2OrderByIdAsc(regionLevel2);
            if (matchedRegionLevel1 != null) {
                Optional<Region> explicitRegionCandidate = candidates.stream()
                        .filter(region -> region.getRegionLevel1()
                                .equals(matchedRegionLevel1))
                        .findFirst();
                if (explicitRegionCandidate.isPresent()) {
                    return withRegion(profile, explicitRegionCandidate.get());
                }
                continue;
            }
            Optional<Region> profileRegionCandidate = candidates.stream()
                    .filter(region -> region.getRegionLevel1()
                            .equals(profile.regionLevel1()))
                    .findFirst();
            if (profileRegionCandidate.isPresent()) {
                return withRegion(profile, profileRegionCandidate.get());
            }
            if (candidates.size() == 1) {
                return withRegion(profile, candidates.getFirst());
            }
            if (candidates.size() > 1) {
                log.info(
                        "[AI] 질문의 지역명이 모호하여 프로필 지역을 유지합니다: "
                                + "regionLevel2={}, candidateCount={}",
                        regionLevel2,
                        candidates.size()
                );
            }
        }
        if (matchedRegionLevel1 != null) {
            return profile.withRegion(
                    matchedRegionLevel1,
                    matchedRegionLevel1,
                    ""
            );
        }
        return profile;
    }

    private AiUserProfile withRegion(AiUserProfile profile, Region region) {
        return profile.withRegion(
                region.getFullName(),
                region.getRegionLevel1(),
                region.getRegionLevel2()
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
        for (String query : distinctQueries) {
            documentsByQuery.add(documentRetriever.retrieve(query, profile, searchScope));
        }

        LinkedHashMap<String, AiReferenceDocument> documentsByKey = new LinkedHashMap<>();
        int maxRank = documentsByQuery.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);
        for (int rank = 0;
             rank < maxRank && documentsByKey.size() < MAX_RETRIEVED_DOCUMENTS;
             rank++) {
            for (List<AiReferenceDocument> queryDocuments : documentsByQuery) {
                if (rank < queryDocuments.size()) {
                    AiReferenceDocument document = queryDocuments.get(rank);
                    documentsByKey.putIfAbsent(document.documentKey(), document);
                }
                if (documentsByKey.size() >= MAX_RETRIEVED_DOCUMENTS) {
                    break;
                }
            }
        }

        List<AiReferenceDocument> merged = documentsByKey.values().stream()
                .limit(MAX_RETRIEVED_DOCUMENTS)
                .toList();
        log.info(
                "[AI] 다중 검색 완료: queryCount={}, uniqueDocumentCount={}",
                distinctQueries.size(),
                merged.size()
        );
        return referenceDocumentResolver.resolve(merged);
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
            List<String> retrievalQueries
    ) {
    }

}
