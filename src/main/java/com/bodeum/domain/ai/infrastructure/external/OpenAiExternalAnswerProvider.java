package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.entity.AiExternalDocument;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.enums.AiExternalSourceType;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.generation.AiPromptFormatter;
import com.bodeum.domain.ai.infrastructure.support.AiPromptTemplate;
import com.bodeum.domain.ai.infrastructure.support.AiSiteDomainNormalizer;
import com.bodeum.domain.ai.infrastructure.support.AiTimeoutDetector;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.repository.AiExternalSourceRepository;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!test")
@Slf4j
public class OpenAiExternalAnswerProvider implements AiExternalAnswerProvider {

    private static final int MAX_ALLOWED_DOMAINS = 100;
    private static final String NO_EVIDENCE_MARKER = "[[NO_EVIDENCE]]";
    private static final String SITE_SOURCE_UNVERIFIED_MESSAGE =
            "사이트별 출처를 정확히 확인하지 못했습니다.";
    private static final Pattern SITE_LIST_QUESTION_PATTERN = Pattern.compile(
            "(사이트|홈페이지).*(알려|추천|목록|모아|찾아)|"
                    + "(알려|추천|목록|모아|찾아).*(사이트|홈페이지)");
    private static final Pattern EXPLICIT_SITE_COUNT_PATTERN = Pattern.compile(
            "(?:사이트|홈페이지).{0,40}?(\\d+)(?:곳|개)|"
                    + "(\\d+)(?:곳|개).{0,40}?(?:사이트|홈페이지)");
    private static final Pattern NUMBERED_LIST_ITEM_PATTERN = Pattern.compile(
            "(?m)^\\s*\\d+[.)]\\s+");
    private static final Pattern REQUESTED_RESULT_COUNT_PATTERN = Pattern.compile(
            "요청 결과 개수:\\s*(\\d+)개");
    private static final String EXCLUDED_RESULTS_MARKER = "이전에 안내하여 제외할 기관:";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiExternalSourceRepository externalSourceRepository;
    private final AiExternalDocumentPersistenceService externalDocumentPersistenceService;
    private final RestClient restClient;
    private final String model;
    private final int maxOutputTokens;
    private final int defaultResultCount;
    private final int maxResultCount;
    private final String externalSearchSystemPrompt;
    private final AiPromptFormatter promptFormatter;

    public OpenAiExternalAnswerProvider(
            AiExternalSourceRepository externalSourceRepository,
            AiExternalDocumentPersistenceService externalDocumentPersistenceService,
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${bodeum.ai.web-search.model:gpt-5.4-mini}") String model,
            @Value("${bodeum.ai.web-search.max-output-tokens:1200}") int maxOutputTokens,
            @Value("${bodeum.ai.web-search.connect-timeout:3s}") Duration connectTimeout,
            @Value("${bodeum.ai.web-search.read-timeout:30s}") Duration readTimeout,
            @Value("${bodeum.ai.result.default-count:5}") int defaultResultCount,
            @Value("${bodeum.ai.result.max-count:10}") int maxResultCount,
            @Value("classpath:prompts/ai-external-search-system-prompt.txt") Resource promptResource,
            AiPromptFormatter promptFormatter
    ) {
        this.externalSourceRepository = externalSourceRepository;
        this.externalDocumentPersistenceService = externalDocumentPersistenceService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .baseUrl("https://api.openai.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
        this.model = model;
        this.maxOutputTokens = maxOutputTokens;
        this.defaultResultCount = defaultResultCount;
        this.maxResultCount = maxResultCount;
        this.externalSearchSystemPrompt = AiPromptTemplate.replaceRequiredPlaceholder(
                readPrompt(promptResource),
                "{{maxResultCount}}",
                Integer.toString(maxResultCount)
        );
        this.promptFormatter = promptFormatter;
    }

    @Override
    public ExternalAiAnswer search(
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        List<AiExternalSource> sources = externalSourceRepository
                .findAllBySourceTypeAndActiveTrue(AiExternalSourceType.WEBSITE)
                .stream()
                .sorted(Comparator.comparing(source -> source.getAuthorityLevel().ordinal()))
                .toList();
        Map<String, AiExternalSource> sourcesByDomain = indexByDomain(sources);
        log.info("[AI] 외부 검색 허용 도메인 수: {}", sourcesByDomain.size());
        if (sourcesByDomain.isEmpty()) {
            log.warn("[AI] 활성화된 외부 검색 허용 도메인이 없습니다.");
            return ExternalAiAnswer.empty();
        }

        try {
            Map<String, Object> body = requestBody(
                    question,
                    retrievalQueries,
                    profile,
                    searchScope,
                    sourcesByDomain.keySet().stream().toList()
            );
            ExternalAiAnswer answer = executeSearch(body, sourcesByDomain);
            if (!isSiteListQuestion(question)) {
                return answer;
            }
            if (!isValidExternalSiteListAnswer(question, answer)) {
                log.warn("[AI] 외부 사이트 목록과 인용 도메인 불일치, 검증된 출처로 재구성합니다.");
            }
            return groupExternalSiteAnswer(question, answer, sourcesByDomain);
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            if (AiTimeoutDetector.isTimeout(e)) {
                throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT, e);
            }
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }

    private ExternalAiAnswer executeSearch(
            Map<String, Object> body,
            Map<String, AiExternalSource> sourcesByDomain
    ) throws IOException {
        String responseBody = restClient.post()
                .uri("/v1/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);
        JsonNode response = responseBody == null
                ? null
                : OBJECT_MAPPER.readTree(responseBody);
        return mapResponse(response, sourcesByDomain);
    }

    private ExternalAiAnswer groupExternalSiteAnswer(
            String question,
            ExternalAiAnswer answer,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        if (answer == null || !answer.hasEvidence()) {
            return ExternalAiAnswer.noEvidence(SITE_SOURCE_UNVERIFIED_MESSAGE);
        }

        Map<String, List<AiReferenceDocument>> referencesByHost = answer.sources().stream()
                .filter(source -> normalizedHost(source.url()) != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        source -> normalizedHost(source.url()),
                        LinkedHashMap::new,
                        java.util.stream.Collectors.toList()
                ));
        if (referencesByHost.isEmpty()) {
            return ExternalAiAnswer.noEvidence(SITE_SOURCE_UNVERIFIED_MESSAGE);
        }

        int requestedCount = requestedResultCount(question);
        int resultLimit = requestedCount > 0
                ? Math.min(requestedCount, maxResultCount)
                : Math.min(Math.max(1, defaultResultCount), maxResultCount);
        List<List<AiReferenceDocument>> selectedReferences = referencesByHost.values().stream()
                .limit(resultLimit)
                .toList();
        int actualCount = selectedReferences.size();
        StringBuilder content = new StringBuilder(countMessage(
                question, requestedCount, actualCount));
        int index = 1;
        for (List<AiReferenceDocument> references : selectedReferences) {
            AiReferenceDocument first = references.getFirst();
            AiExternalSource registeredSource = findSource(
                    first.url(), sourcesByDomain).orElse(null);
            String siteName = registeredSource == null
                    ? first.title()
                    : registeredSource.getName();
            if (siteName == null || siteName.isBlank()) {
                return ExternalAiAnswer.noEvidence(SITE_SOURCE_UNVERIFIED_MESSAGE);
            }

            content.append("\n\n")
                    .append(index++)
                    .append(". **")
                    .append(siteName.trim())
                    .append("**");
            List<String> pageTitles = references.stream()
                    .map(AiReferenceDocument::title)
                    .filter(title -> title != null && !title.isBlank())
                    .map(String::trim)
                    .filter(title -> !title.equalsIgnoreCase(siteName.trim()))
                    .distinct()
                    .toList();
            if (!pageTitles.isEmpty()) {
                content.append("\n\n- 관련 안내 페이지");
                pageTitles.forEach(title -> content.append("\n  - ").append(title));
            }
        }
        return new ExternalAiAnswer(
                content.toString(),
                selectedReferences.stream()
                        .flatMap(List::stream)
                        .map(reference -> canonicalSiteReference(
                                reference, sourcesByDomain))
                        .toList(),
                answer.answerStatus()
        );
    }

    private AiReferenceDocument canonicalSiteReference(
            AiReferenceDocument reference,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        String siteName = findSource(reference.url(), sourcesByDomain)
                .map(AiExternalSource::getName)
                .filter(name -> !name.isBlank())
                .orElse(reference.title());
        return new AiReferenceDocument(
                reference.documentKey(), reference.content(), reference.sourceType(),
                reference.sourceId(), siteName, reference.url(), reference.updatedAt());
    }

    private static String countMessage(
            String question,
            int requestedCount,
            int actualCount
    ) {
        boolean additionalResults = question != null
                && question.contains(EXCLUDED_RESULTS_MARKER);
        if (requestedCount > actualCount) {
            if (additionalResults) {
                return "요청하신 " + requestedCount
                        + "곳 중 이전에 안내한 항목을 제외하고 현재 추가로 확인 가능한 "
                        + "공식 사이트는 " + actualCount + "곳입니다.";
            }
            return "요청하신 " + requestedCount
                    + "곳 중 현재 확인 가능한 공식 사이트는 "
                    + actualCount + "곳입니다.";
        }
        if (additionalResults) {
            return "이전에 안내한 항목을 제외하면, 추가로 확인 가능한 공식 사이트는 "
                    + actualCount + "곳입니다.";
        }
        return "현재 확인 가능한 공식 사이트는 " + actualCount + "곳입니다.";
    }

    private static int requestedResultCount(String question) {
        if (question == null || question.isBlank()) {
            return 0;
        }
        Matcher matcher = REQUESTED_RESULT_COUNT_PATTERN.matcher(question);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    static boolean isValidExternalSiteListAnswer(
            String question,
            ExternalAiAnswer answer
    ) {
        if (!isSiteListQuestion(question) || answer == null || !answer.hasEvidence()) {
            return true;
        }

        Set<String> citedHosts = new HashSet<>();
        for (AiReferenceDocument source : answer.sources()) {
            String host = normalizedHost(source.url());
            if (host != null) {
                citedHosts.add(host);
            }
        }
        if (citedHosts.isEmpty()) {
            return false;
        }

        int claimedCount = Math.max(
                explicitSiteCount(answer.answer()),
                numberedListItemCount(answer.answer())
        );
        return claimedCount == 0 || claimedCount <= citedHosts.size();
    }

    private static boolean isSiteListQuestion(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        String normalized = question.replaceAll("\\s+", "");
        return SITE_LIST_QUESTION_PATTERN.matcher(normalized).find();
    }

    private static int explicitSiteCount(String answer) {
        Matcher matcher = EXPLICIT_SITE_COUNT_PATTERN.matcher(answer);
        int count = 0;
        while (matcher.find()) {
            String value = matcher.group(1) == null ? matcher.group(2) : matcher.group(1);
            try {
                count = Math.max(count, Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                return Integer.MAX_VALUE;
            }
        }
        return count;
    }

    private static int numberedListItemCount(String answer) {
        Matcher matcher = NUMBERED_LIST_ITEM_PATTERN.matcher(answer);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String normalizedHost(String url) {
        return AiSiteDomainNormalizer.normalize(url);
    }

    Map<String, Object> requestBody(
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope,
            List<String> allowedDomains
    ) {
        Map<String, Object> filters = Map.of(
                "allowed_domains", allowedDomains.stream().limit(MAX_ALLOWED_DOMAINS).toList());
        Map<String, Object> webSearch = new LinkedHashMap<>();
        webSearch.put("type", "web_search");
        webSearch.put("search_context_size", "medium");
        webSearch.put("filters", filters);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_output_tokens", maxOutputTokens);
        body.put("tools", List.of(webSearch));
        body.put("tool_choice", "required");
        body.put("include", List.of("web_search_call.action.sources"));
        body.put("instructions", externalSearchSystemPrompt);
        body.put("input", externalSearchInput(
                question, retrievalQueries, profile, searchScope));
        return body;
    }

    private String externalSearchInput(
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        AiUserProfile searchProfile = searchScope == AiSearchScope.REGION_PRIORITY
                ? profile.withRegion("", "", "")
                : profile;
        String searchHints = retrievalQueries == null
                ? ""
                : retrievalQueries.stream()
                        .filter(query -> query != null && !query.isBlank())
                        .map(String::trim)
                        .distinct()
                        .limit(3)
                        .map(query -> "- " + externalSearchQuery(
                                query, searchProfile, searchScope))
                        .collect(java.util.stream.Collectors.joining("\n"));
        return """
                %s

                [우선 확인할 공식 페이지]
                %s

                [검색 질의 힌트]
                %s

                [사용자 질문]
                %s
                """.formatted(
                promptFormatter.formatProfile(searchProfile),
                "등록된 허용 도메인에서 관련 상세 페이지를 찾으세요.",
                searchHints.isBlank()
                        ? "- " + externalSearchQuery(question, searchProfile, searchScope)
                        : searchHints,
                question
        );
    }

    private String externalSearchQuery(
            String query,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (searchScope != AiSearchScope.LOCAL_ONLY
                || profile == null
                || profile.region() == null
                || profile.region().isBlank()) {
            return query;
        }
        if (query.contains(profile.region())) {
            return query;
        }
        return profile.region() + " " + query;
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("AI 외부 검색 프롬프트를 읽을 수 없습니다.", e);
        }
    }

    private ExternalAiAnswer mapResponse(
            JsonNode response,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        if (response == null) {
            return ExternalAiAnswer.empty();
        }

        for (JsonNode output : response.path("output")) {
            if (!"message".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode content : output.path("content")) {
                if (!"output_text".equals(content.path("type").asText())) {
                    continue;
                }
                String answer = content.path("text").asText(null);
                if (answer == null || answer.isBlank() || isNoEvidenceAnswer(answer)) {
                    log.info("[AI] 외부 검색에서 상세 근거를 확인하지 못했습니다.");
                    return ExternalAiAnswer.empty();
                }
                List<AiReferenceDocument> references = mapCitations(
                        content.path("annotations"), sourcesByDomain);
                log.info("[AI] 외부 검색 유효 인용 수: {}", references.size());
                if (references.isEmpty()) {
                    // 검색 과정에서 방문한 URL은 답변 근거로 확정할 수 없다.
                    // 실제 citation이 없으면, ANSWERED가 아닌 LINK_GUIDANCE로만 안내한다.
                    List<AiReferenceDocument> searchSources = mapSearchSources(
                            response.path("output"), sourcesByDomain);
                    log.info("[AI] 외부 검색 fallback URL 수: {}", searchSources.size());
                    return linkGuidance(
                            searchSources,
                            sourcesByDomain
                    );
                }
                return new ExternalAiAnswer(answer, references);
            }
        }
        return ExternalAiAnswer.empty();
    }

    static boolean isNoEvidenceAnswer(String answer) {
        String normalized = answer.strip();
        return normalized.contains(NO_EVIDENCE_MARKER)
                || normalized.contains("찾지 못했습니다")
                || normalized.contains("확인하지 못했습니다")
                || normalized.contains("확인되지 않았습니다")
                || normalized.contains("확인할 수 없습니다");
    }

    private ExternalAiAnswer linkGuidance(
            List<AiReferenceDocument> fallbackSources,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        if (fallbackSources.isEmpty()) {
            return ExternalAiAnswer.empty();
        }
        AiReferenceDocument source = fallbackSources.getFirst();
        AiExternalSource externalSource = findSource(source.url(), sourcesByDomain).orElse(null);
        if (externalSource == null) {
            return ExternalAiAnswer.empty();
        }
        return ExternalAiAnswer.linkGuidance(
                "관련 상세 내용을 확인하지 못했습니다. %s에서 직접 확인해 주세요."
                        .formatted(externalSource.getName()),
                List.of(source)
        );
    }

    private List<AiReferenceDocument> mapSearchSources(
            JsonNode outputs,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        Map<String, AiExternalDocumentCandidate> candidatesByUrl = new LinkedHashMap<>();
        for (JsonNode output : outputs) {
            if (!"web_search_call".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode source : output.path("action").path("sources")) {
                String url = source.path("url").asText(null);
                if (url == null) {
                    continue;
                }
                String normalizedUrl = AiUrlNormalizer.normalize(url);
                AiExternalSource externalSource = findSource(normalizedUrl, sourcesByDomain).orElse(null);
                if (externalSource == null) {
                    continue;
                }
                String title = source.path("title").asText(externalSource.getName());
                candidatesByUrl.putIfAbsent(normalizedUrl, new AiExternalDocumentCandidate(
                        externalSource, title, normalizedUrl, sha256(normalizedUrl)));
            }
        }
        return saveResources(candidatesByUrl.values());
    }

    private List<AiReferenceDocument> mapCitations(
            JsonNode annotations,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        // 답변 본문에 연결된 url_citation 중 사전에 등록된 허용 도메인만 보존한다.
        Map<String, AiExternalDocumentCandidate> candidatesByUrl = new LinkedHashMap<>();
        for (JsonNode annotation : annotations) {
            if (!"url_citation".equals(annotation.path("type").asText())) {
                continue;
            }
            String url = annotation.path("url").asText(null);
            String title = annotation.path("title").asText(null);
            if (url == null || title == null || title.isBlank()) {
                continue;
            }
            String normalizedUrl = AiUrlNormalizer.normalize(url);
            AiExternalSource externalSource = findSource(normalizedUrl, sourcesByDomain).orElse(null);
            if (externalSource == null) {
                continue;
            }
            candidatesByUrl.putIfAbsent(normalizedUrl, new AiExternalDocumentCandidate(
                    externalSource, title, normalizedUrl, sha256(normalizedUrl)));
        }
        return saveResources(candidatesByUrl.values());
    }

    private AiReferenceDocument toReference(AiExternalDocument document) {
        return new AiReferenceDocument(
                "SITE-" + document.getId(),
                document.getTitle(),
                AiResponseSourceType.SITE,
                document.getId(),
                document.getTitle(),
                document.getSourceUrl(),
                document.getSourceUpdatedAt()
        );
    }

    private List<AiReferenceDocument> saveResources(
            java.util.Collection<AiExternalDocumentCandidate> candidates
    ) {
        return externalDocumentPersistenceService.saveAll(candidates).stream()
                .map(this::toReference)
                .toList();
    }

    private Map<String, AiExternalSource> indexByDomain(List<AiExternalSource> sources) {
        Map<String, AiExternalSource> indexed = new LinkedHashMap<>();
        for (AiExternalSource source : sources) {
            String domain = URI.create(source.getBaseUrl()).getHost();
            if (domain != null && !domain.isBlank()) {
                indexed.putIfAbsent(domain.toLowerCase(Locale.ROOT), source);
            }
        }
        return indexed;
    }

    private Optional<AiExternalSource> findSource(
            String url,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        String host = URI.create(url).getHost();
        if (host == null) {
            return Optional.empty();
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        return sourcesByDomain.entrySet().stream()
                .filter(entry -> normalizedHost.equals(entry.getKey())
                        || normalizedHost.endsWith("." + entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
