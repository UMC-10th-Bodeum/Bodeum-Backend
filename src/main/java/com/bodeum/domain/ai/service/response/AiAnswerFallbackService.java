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
import com.bodeum.domain.ai.infrastructure.support.AiSiteDomainNormalizer;
import com.bodeum.domain.ai.infrastructure.support.AiSourceDeduplicator;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final AiAnswerResultNormalizer answerResultNormalizer;

    @Value("${bodeum.ai.result.max-count:10}")
    private int maxResultCount = 10;

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
        return externalOrNoResult(
                chatRoom, userMessage, question, retrievalQueries, profile, searchScope,
                null, false, null, null, false, false);
    }

    public CreateAiMessageResponse saveStarterSiteAnswer(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope,
            AiStarterQuestionAnswer starterAnswer,
            Integer requestedResultCount
    ) {
        List<AiReferenceDocument> fixedSources = uniqueSites(starterAnswer.sources());
        int targetCount = requestedResultCount == null
                ? fixedSources.size()
                : Math.min(Math.max(1, requestedResultCount), maxResultCount);
        List<AiReferenceDocument> supplementalSources = List.of();
        if (targetCount > fixedSources.size()) {
            ExternalAiAnswer externalAnswer = externalAnswerProvider.search(
                    question, retrievalQueries, profile, searchScope);
            if (externalAnswer.hasEvidence()) {
                Map<String, AiReferenceDocument> fixedByHost = indexSites(fixedSources);
                supplementalSources = uniqueSites(externalAnswer.sources()).stream()
                        .filter(source -> !fixedByHost.containsKey(siteHost(source)))
                        .limit(targetCount - fixedSources.size())
                        .toList();
            }
        }

        List<AiReferenceDocument> mergedSources = java.util.stream.Stream
                .concat(fixedSources.stream(), supplementalSources.stream())
                .toList();
        String content = removeCuratedSiteCountHeader(starterAnswer.content());
        if (!supplementalSources.isEmpty()) {
            content += "\n\n**추가로 확인한 공식 사이트**\n"
                    + supplementalSources.stream()
                    .map(source -> "- **" + source.title() + "** — " + source.url())
                    .collect(java.util.stream.Collectors.joining("\n"));
        }
        content = answerResultNormalizer.normalizeExternalListAnswer(
                content, mergedSources.size(), requestedResultCount,
                false, null, null, true, true);
        return saveSourceBacked(
                chatRoom, userMessage, content, AiAnswerStatus.ANSWERED, mergedSources);
    }

    /**
     * 내부 사이트 근거를 유지하면서 요청 개수에 부족한 고유 도메인만
     * 외부 검색 결과로 보충한다.
     */
    public List<AiReferenceDocument> supplementSiteSources(
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope,
            List<AiReferenceDocument> internalSources,
            int requestedResultCount
    ) {
        int targetCount = Math.min(Math.max(1, requestedResultCount), maxResultCount);
        List<AiReferenceDocument> uniqueInternalSources = uniqueSites(internalSources);
        if (uniqueInternalSources.size() >= targetCount) {
            return uniqueInternalSources.stream().limit(targetCount).toList();
        }

        ExternalAiAnswer externalAnswer = externalAnswerProvider.search(
                question, retrievalQueries, profile, searchScope);
        if (!externalAnswer.hasEvidence()) {
            return uniqueInternalSources;
        }

        Map<String, AiReferenceDocument> mergedByHost = indexSites(uniqueInternalSources);
        uniqueSites(externalAnswer.sources()).forEach(source ->
                mergedByHost.putIfAbsent(siteHost(source), source));
        return mergedByHost.values().stream().limit(targetCount).toList();
    }

    private List<AiReferenceDocument> uniqueSites(List<AiReferenceDocument> sources) {
        return List.copyOf(indexSites(sources).values());
    }

    private Map<String, AiReferenceDocument> indexSites(List<AiReferenceDocument> sources) {
        Map<String, AiReferenceDocument> sitesByHost = new LinkedHashMap<>();
        if (sources == null) {
            return sitesByHost;
        }
        sources.forEach(source -> {
            String host = siteHost(source);
            if (host != null) {
                sitesByHost.putIfAbsent(host, source);
            }
        });
        return sitesByHost;
    }

    private String siteHost(AiReferenceDocument source) {
        return source == null ? null : AiSiteDomainNormalizer.normalize(source.url());
    }

    private String removeCuratedSiteCountHeader(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceFirst(
                "^네, 참고하면 좋을 공식 (?:복지 )?사이트 \\d+개를 추천드리겠습니다!\\s*",
                ""
        ).trim();
    }

    public CreateAiMessageResponse externalOrNoResult(
            AiChatRoom chatRoom,
            AiMessage userMessage,
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory,
            String region,
            boolean listRequest,
            boolean siteListRequest
    ) {
        ExternalAiAnswer answer = externalAnswerProvider.search(
                question, retrievalQueries, profile, searchScope);
        if (!answer.hasEvidence()) {
            String content = answer.answer() == null || answer.answer().isBlank()
                    ? NO_RESULT_MESSAGE : answer.answer();
            return noEvidence(chatRoom, userMessage, content);
        }
        String content = answerResultNormalizer.normalizeExternalListAnswer(
                answer.answer(), answer.sources().size(), requestedResultCount,
                additionalResults, infoSubCategory, region, listRequest, siteListRequest);
        return saveSourceBacked(chatRoom, userMessage, content,
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
        List<AiReferenceDocument> distinctSources = AiSourceDeduplicator.deduplicate(sources);
        boolean warning = evidenceService.hasIncorrectFeedback(distinctSources);
        List<AiMessageSourceResponse> sourceResponses = distinctSources.stream()
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
        List<AiReferenceDocument> distinctSources = AiSourceDeduplicator.deduplicate(sources);
        boolean warning = evidenceService.hasIncorrectFeedback(distinctSources);
        AiMessage message = persistenceService.saveAiMessageAndComplete(
                userMessage.getId(), chatRoom, content, warning, answerStatus, distinctSources);
        return sourceBacked(message, distinctSources, answerStatus);
    }
}
