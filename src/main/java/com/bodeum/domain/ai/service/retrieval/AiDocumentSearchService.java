package com.bodeum.domain.ai.service.retrieval;

import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;
import com.bodeum.domain.ai.util.AiTextNormalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 질문과 확장 검색어로 RAG 문서를 조회하고,
 * 근거 적합성 검증과 기관 중복 제거를 거쳐 검색 결과를 구성한다.
 */
@Service
@Slf4j
public class AiDocumentSearchService {

    private final AiDocumentRetriever documentRetriever;
    private final AiReferenceDocumentResolver referenceDocumentResolver;
    private final AiAnswerEvidenceService evidenceService;
    private final int maxResultCount;
    private final int maxSupplementalConceptSearches;
    @Value("${bodeum.ai.rag.max-candidate-count:30}")
    private int maxCandidateCount = 30;

    public AiDocumentSearchService(
            AiDocumentRetriever documentRetriever,
            AiReferenceDocumentResolver referenceDocumentResolver,
            @Value("${bodeum.ai.result.max-count:10}") int maxResultCount,
            @Value("${bodeum.ai.rag.max-supplemental-concept-searches:3}")
            int maxSupplementalConceptSearches,
            AiAnswerEvidenceService evidenceService
    ) {
        this.documentRetriever = documentRetriever;
        this.referenceDocumentResolver = referenceDocumentResolver;
        this.evidenceService = evidenceService;
        this.maxResultCount = maxResultCount;
        this.maxSupplementalConceptSearches = maxSupplementalConceptSearches;
    }

    public List<AiReferenceDocument> retrieve(
            String originalQuestion,
            List<String> expandedQueries,
            String searchGoal,
            List<AiRequiredConcept> requiredConcepts,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        return retrieve(
                originalQuestion, expandedQueries, searchGoal, requiredConcepts,
                profile, searchScope, null, AiAdditionalResultsContext.empty());
    }

    public List<AiReferenceDocument> retrieve(
            String originalQuestion,
            List<String> expandedQueries,
            String searchGoal,
            List<AiRequiredConcept> requiredConcepts,
            AiUserProfile profile,
            AiSearchScope searchScope,
            Integer requestedResultCount,
            AiAdditionalResultsContext additionalResults
    ) {
        AiAdditionalResultsContext additionalContext = additionalResults == null
                ? AiAdditionalResultsContext.empty() : additionalResults;
        int targetCount = requestedResultCount == null
                ? maxResultCount
                : Math.min(Math.max(1, requestedResultCount), maxResultCount);
        int candidateLimit = Math.max(maxResultCount, maxCandidateCount);
        int candidateCount = additionalContext.isFollowUp()
                ? Math.min(Math.max(targetCount * 3, maxResultCount), candidateLimit)
                : targetCount;
        List<String> queries = new ArrayList<>();
        queries.add(originalQuestion);
        if (expandedQueries != null) {
            queries.addAll(expandedQueries);
        }
        List<String> distinctQueries = queries.stream()
                .filter(query -> query != null && !query.isBlank())
                .map(this::semanticSearchQuestion)
                .distinct()
                .limit(3)
                .toList();

        List<List<AiReferenceDocument>> documentsByQuery = new ArrayList<>();
        for (int queryIndex = 0; queryIndex < distinctQueries.size(); queryIndex++) {
            List<AiReferenceDocument> queryDocuments = additionalContext.isFollowUp()
                    ? documentRetriever.retrieve(
                            distinctQueries.get(queryIndex), profile, searchScope, candidateCount)
                    : documentRetriever.retrieve(
                            distinctQueries.get(queryIndex), profile, searchScope);
            queryDocuments = excludePreviousResults(queryDocuments, additionalContext);
            documentsByQuery.add(queryDocuments);
            log.debug("[AI] 질의별 검색 결과: queryIndex={}, documentKeys={}",
                    queryIndex,
                    queryDocuments.stream().map(AiReferenceDocument::documentKey).toList());
        }

        LinkedHashMap<String, AiReferenceDocument> documentsByKey = new LinkedHashMap<>();
        preserveRequiredConceptDocuments(
                requiredConcepts, searchGoal, documentsByQuery, documentsByKey,
                profile, searchScope, additionalContext, candidateCount);
        if (AiTextNormalizer.removeWhitespace(originalQuestion).contains("장애아동")) {
            mergeRoundRobin(documentsByQuery, documentsByKey, targetCount);
        } else {
            mergeOriginalFirst(documentsByQuery, documentsByKey, targetCount);
        }

        List<AiReferenceDocument> merged = documentsByKey.values().stream()
                .limit(targetCount)
                .toList();
        log.info("[AI] 다중 검색 완료: queryCount={}, uniqueDocumentCount={}",
                distinctQueries.size(), merged.size());
        return referenceDocumentResolver.resolve(merged);
    }

    private void preserveRequiredConceptDocuments(
            List<AiRequiredConcept> requiredConcepts,
            String searchGoal,
            List<List<AiReferenceDocument>> documentsByQuery,
            LinkedHashMap<String, AiReferenceDocument> documentsByKey,
            AiUserProfile profile,
            AiSearchScope searchScope,
            AiAdditionalResultsContext additionalContext,
            int candidateCount
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
                    candidates, concept, profile, documentsByKey.keySet());
            if (matched.isEmpty()
                    && supplementalSearchCount < maxSupplementalConceptSearches) {
                supplementalSearchCount++;
                List<AiReferenceDocument> supplemented = additionalContext.isFollowUp()
                        ? documentRetriever.retrieve(
                                supplementalResultQuery(concept, searchGoal, profile),
                                profile, searchScope, candidateCount)
                        : documentRetriever.retrieve(
                                supplementalResultQuery(concept, searchGoal, profile),
                                profile, searchScope);
                supplemented = excludePreviousResults(supplemented, additionalContext);
                candidates.addAll(supplemented);
                matched = findConceptDocument(
                        supplemented, concept, profile, documentsByKey.keySet());
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
                .map(AiTextNormalizer::removeWhitespace).toList();
        List<String> excludeTerms = concept.excludeTerms().stream()
                .map(AiTextNormalizer::removeWhitespace).toList();
        return documents.stream()
                .filter(document -> !excludedDocumentKeys.contains(document.documentKey()))
                .filter(document -> {
                    String searchableText = AiTextNormalizer.removeWhitespace(document.title())
                            + AiTextNormalizer.removeWhitespace(document.content());
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
        String region = AiTextNormalizer.removeWhitespace(profile.region());
        String regionLevel2 = AiTextNormalizer.removeWhitespace(profile.regionLevel2());
        return (!region.isBlank() && searchableText.contains(region))
                || (!regionLevel2.isBlank() && searchableText.contains(regionLevel2));
    }

    private void mergeRoundRobin(
            List<List<AiReferenceDocument>> documentsByQuery,
            LinkedHashMap<String, AiReferenceDocument> documentsByKey,
            int targetCount
    ) {
        int maxRank = documentsByQuery.stream().mapToInt(List::size).max().orElse(0);
        for (int rank = 0; rank < maxRank && documentsByKey.size() < targetCount; rank++) {
            for (List<AiReferenceDocument> queryDocuments : documentsByQuery) {
                if (rank < queryDocuments.size()) {
                    AiReferenceDocument document = queryDocuments.get(rank);
                    documentsByKey.putIfAbsent(document.documentKey(), document);
                }
                if (documentsByKey.size() >= targetCount) {
                    break;
                }
            }
        }
    }

    private void mergeOriginalFirst(
            List<List<AiReferenceDocument>> documentsByQuery,
            LinkedHashMap<String, AiReferenceDocument> documentsByKey,
            int targetCount
    ) {
        if (!documentsByQuery.isEmpty()) {
            int originalLimit = targetCount - Math.min(
                    documentsByQuery.size() - 1, targetCount);
            for (AiReferenceDocument document : documentsByQuery.getFirst()) {
                documentsByKey.putIfAbsent(document.documentKey(), document);
                if (documentsByKey.size() >= originalLimit) {
                    break;
                }
            }
        }
        for (int queryIndex = 1;
             queryIndex < documentsByQuery.size() && documentsByKey.size() < targetCount;
             queryIndex++) {
            List<AiReferenceDocument> queryDocuments = documentsByQuery.get(queryIndex);
            if (!queryDocuments.isEmpty()) {
                AiReferenceDocument document = queryDocuments.getFirst();
                documentsByKey.putIfAbsent(document.documentKey(), document);
            }
        }
        int maxExpandedRank = documentsByQuery.stream()
                .skip(1).mapToInt(List::size).max().orElse(0);
        for (int rank = 1;
             rank < maxExpandedRank && documentsByKey.size() < targetCount;
             rank++) {
            for (int queryIndex = 1; queryIndex < documentsByQuery.size(); queryIndex++) {
                List<AiReferenceDocument> queryDocuments = documentsByQuery.get(queryIndex);
                if (rank < queryDocuments.size()) {
                    AiReferenceDocument document = queryDocuments.get(rank);
                    documentsByKey.putIfAbsent(document.documentKey(), document);
                }
                if (documentsByKey.size() >= targetCount) {
                    break;
                }
            }
        }
        if (!documentsByQuery.isEmpty()) {
            for (AiReferenceDocument document : documentsByQuery.getFirst()) {
                documentsByKey.putIfAbsent(document.documentKey(), document);
                if (documentsByKey.size() >= targetCount) {
                    break;
                }
            }
        }
    }

    private List<AiReferenceDocument> excludePreviousResults(
            List<AiReferenceDocument> documents,
            AiAdditionalResultsContext context
    ) {
        if (!context.isFollowUp()) {
            return documents;
        }
        return documents.stream()
                .filter(document -> !context.excludedSources().contains(
                        new AiSourceKey(document.sourceType(), document.sourceId())))
                .filter(document -> context.excludedIdentityKeys().isEmpty()
                        || evidenceService.documentIdentityKeys(document).stream()
                        .noneMatch(context.excludedIdentityKeys()::contains))
                .toList();
    }

    private String semanticSearchQuestion(String question) {
        return question.lines()
                .filter(line -> !line.startsWith("검색 후보 개수:"))
                .filter(line -> !line.startsWith("이전에 안내하여 제외할 기관:"))
                .collect(java.util.stream.Collectors.joining("\n"))
                .trim();
    }

}
