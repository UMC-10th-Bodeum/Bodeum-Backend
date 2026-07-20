package com.bodeum.domain.ai.infrastructure.external;

import com.bodeum.domain.ai.entity.AiExternalResource;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.enums.AiExternalSourceType;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.generation.AiPromptFormatter;
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
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
public class OpenAiExternalAnswerProvider implements AiExternalAnswerProvider {

    private static final int MAX_ALLOWED_DOMAINS = 100;
    private static final String NO_EVIDENCE_MARKER = "[[NO_EVIDENCE]]";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiExternalSourceRepository externalSourceRepository;
    private final AiExternalResourcePersistenceService externalResourcePersistenceService;
    private final RestClient restClient;
    private final String model;
    private final int maxOutputTokens;
    private final String externalSearchSystemPrompt;
    private final AiPromptFormatter promptFormatter;

    public OpenAiExternalAnswerProvider(
            AiExternalSourceRepository externalSourceRepository,
            AiExternalResourcePersistenceService externalResourcePersistenceService,
            RestClient.Builder restClientBuilder,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${bodeum.ai.web-search.model:gpt-5.4-mini}") String model,
            @Value("${bodeum.ai.web-search.max-output-tokens:1200}") int maxOutputTokens,
            @Value("${bodeum.ai.web-search.connect-timeout:3s}") Duration connectTimeout,
            @Value("${bodeum.ai.web-search.read-timeout:30s}") Duration readTimeout,
            @Value("classpath:prompts/ai-external-search-system-prompt.txt") Resource promptResource,
            AiPromptFormatter promptFormatter
    ) {
        this.externalSourceRepository = externalSourceRepository;
        this.externalResourcePersistenceService = externalResourcePersistenceService;
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
        this.externalSearchSystemPrompt = readPrompt(promptResource);
        this.promptFormatter = promptFormatter;
    }

    @Override
    public ExternalAiAnswer search(String question, AiUserProfile profile) {
        List<AiExternalSource> sources = externalSourceRepository
                .findAllBySourceTypeAndActiveTrue(AiExternalSourceType.WEBSITE)
                .stream()
                .sorted(Comparator.comparing(source -> source.getAuthorityLevel().ordinal()))
                .toList();
        Map<String, AiExternalSource> sourcesByDomain = indexByDomain(sources);
        if (sourcesByDomain.isEmpty()) {
            return ExternalAiAnswer.empty();
        }

        try {
            String responseBody = restClient.post()
                    .uri("/v1/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody(question, profile, sourcesByDomain.keySet().stream().toList()))
                    .retrieve()
                    .body(String.class);
            JsonNode response = responseBody == null
                    ? null
                    : OBJECT_MAPPER.readTree(responseBody);
            return mapResponse(response, sourcesByDomain);
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            if (AiTimeoutDetector.isTimeout(e)) {
                throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT, e);
            }
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }

    private Map<String, Object> requestBody(
            String question,
            AiUserProfile profile,
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
        body.put("input", externalSearchPrompt(question, profile));
        return body;
    }

    private String externalSearchPrompt(String question, AiUserProfile profile) {
        return """
                %s

                %s

                [사용자 질문]
                %s
                """.formatted(
                externalSearchSystemPrompt,
                promptFormatter.formatProfile(profile),
                question
        );
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
                    return ExternalAiAnswer.empty();
                }
                List<AiReferenceDocument> references = mapCitations(
                        content.path("annotations"), sourcesByDomain);
                if (references.isEmpty()) {
                    return linkGuidance(
                            mapSearchSources(response.path("output"), sourcesByDomain),
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
        Map<String, AiExternalResourceCandidate> candidatesByUrl = new LinkedHashMap<>();
        for (JsonNode output : outputs) {
            if (!"web_search_call".equals(output.path("type").asText())) {
                continue;
            }
            for (JsonNode source : output.path("action").path("sources")) {
                String url = source.path("url").asText(null);
                if (url == null) {
                    continue;
                }
                String normalizedUrl = normalizeUrl(url);
                AiExternalSource externalSource = findSource(normalizedUrl, sourcesByDomain).orElse(null);
                if (externalSource == null) {
                    continue;
                }
                String title = source.path("title").asText(externalSource.getName());
                candidatesByUrl.putIfAbsent(normalizedUrl, new AiExternalResourceCandidate(
                        externalSource, title, normalizedUrl, sha256(normalizedUrl)));
            }
        }
        return saveResources(candidatesByUrl.values());
    }

    private List<AiReferenceDocument> mapCitations(
            JsonNode annotations,
            Map<String, AiExternalSource> sourcesByDomain
    ) {
        Map<String, AiExternalResourceCandidate> candidatesByUrl = new LinkedHashMap<>();
        for (JsonNode annotation : annotations) {
            if (!"url_citation".equals(annotation.path("type").asText())) {
                continue;
            }
            String url = annotation.path("url").asText(null);
            String title = annotation.path("title").asText(null);
            if (url == null || title == null || title.isBlank()) {
                continue;
            }
            String normalizedUrl = normalizeUrl(url);
            AiExternalSource externalSource = findSource(normalizedUrl, sourcesByDomain).orElse(null);
            if (externalSource == null) {
                continue;
            }
            candidatesByUrl.putIfAbsent(normalizedUrl, new AiExternalResourceCandidate(
                    externalSource, title, normalizedUrl, sha256(normalizedUrl)));
        }
        return saveResources(candidatesByUrl.values());
    }

    private AiReferenceDocument toReference(AiExternalResource resource) {
        return new AiReferenceDocument(
                "SITE-" + resource.getId(),
                resource.getTitle(),
                AiResponseSourceType.SITE,
                resource.getId(),
                resource.getTitle(),
                resource.getSourceUrl(),
                resource.getSourceUpdatedAt()
        );
    }

    private List<AiReferenceDocument> saveResources(
            java.util.Collection<AiExternalResourceCandidate> candidates
    ) {
        return externalResourcePersistenceService.saveAll(candidates).stream()
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

    private String normalizeUrl(String url) {
        URI uri = URI.create(url).normalize();
        try {
            return new URI(
                    uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT),
                    uri.getUserInfo(),
                    uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            ).toString();
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED);
        }
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
