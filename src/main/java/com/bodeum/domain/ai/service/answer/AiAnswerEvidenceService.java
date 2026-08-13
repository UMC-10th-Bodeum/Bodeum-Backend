package com.bodeum.domain.ai.service.answer;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnswerEvidenceService {

    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile(
            "(?<!\\d)(?:0\\d{1,2})[- .]?\\d{3,4}[- .]?\\d{4}(?!\\d)");

    private final AiSourceReviewRepository aiSourceReviewRepository;

    public List<AiReferenceDocument> validateCitations(
            GeneratedAiAnswer generated,
            List<AiReferenceDocument> retrievedDocuments
    ) {
        Set<String> citedKeys = new HashSet<>(generated.citedDocumentKeys() == null
                ? List.of() : generated.citedDocumentKeys());
        List<AiReferenceDocument> cited = retrievedDocuments.stream()
                .filter(document -> citedKeys.contains(document.documentKey()))
                .toList();
        if (cited.isEmpty()) {
            log.warn("[AI] citation 검증 실패. citedKeys={}, retrievedKeys={}", citedKeys,
                    retrievedDocuments.stream().map(AiReferenceDocument::documentKey).toList());
        }
        return cited;
    }

    public boolean hasIncorrectFeedback(List<AiReferenceDocument> sources) {
        if (sources.isEmpty()) {
            return false;
        }
        Set<AiSourceKey> sourceKeys = sources.stream()
                .map(source -> new AiSourceKey(source.sourceType(), source.sourceId()))
                .collect(java.util.stream.Collectors.toSet());
        return aiSourceReviewRepository.existsWarningRequiredBySources(sourceKeys);
    }

    public List<AiReferenceDocument> deduplicateInstitutions(
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

    public Set<String> documentIdentityKeys(AiReferenceDocument document) {
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

    public Set<String> sourceIdentityKeys(String title, String url) {
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
        return title == null ? "" : title.toLowerCase(Locale.ROOT)
                .replaceAll("^\\s*\\[[^]]+]\\s*", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private String normalizeInstitutionUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(url.contains("://") ? url.trim() : "https://" + url.trim())
                    .normalize();
            if (uri.getHost() == null || uri.getHost().isBlank()) {
                return "";
            }
            String path = uri.getPath() == null ? "" : uri.getPath().replaceAll("/+$", "");
            return uri.getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.", "") + path;
        } catch (IllegalArgumentException ignored) {
            return url.trim().toLowerCase(Locale.ROOT).replaceAll("/+$", "");
        }
    }
}
