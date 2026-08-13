package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.infrastructure.support.AiSiteDomainNormalizer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswerItem;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiSiteListAnswerValidator {

    private static final Pattern SITE_LIST_QUESTION_PATTERN = Pattern.compile(
            "(사이트|홈페이지).*(알려|추천|목록|모아|찾아)|"
                    + "(알려|추천|목록|모아|찾아).*(사이트|홈페이지)");

    public boolean requiresValidation(String question) {
        return question != null
                && SITE_LIST_QUESTION_PATTERN.matcher(normalizeQuestion(question)).find();
    }

    public boolean isValid(
            GeneratedAiAnswer generated,
            List<AiReferenceDocument> retrievedDocuments
    ) {
        List<GeneratedAiAnswerItem> answerItems = generated.answerItems();
        if (answerItems.isEmpty()) {
            return false;
        }

        Set<String> citedKeys = new HashSet<>(generated.citedDocumentKeys());
        Map<String, AiReferenceDocument> documentsByKey = retrievedDocuments.stream()
                .collect(Collectors.toMap(
                        AiReferenceDocument::documentKey,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        Set<String> seenHosts = new HashSet<>();

        for (GeneratedAiAnswerItem item : answerItems) {
            if (item == null || item.name() == null || item.name().isBlank()
                    || item.documentKey() == null || item.documentKey().isBlank()
                    || !citedKeys.contains(item.documentKey())) {
                return false;
            }
            AiReferenceDocument document = documentsByKey.get(item.documentKey());
            String host = AiSiteDomainNormalizer.normalize(
                    document == null ? null : document.url());
            if (host == null || !seenHosts.add(host)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeQuestion(String question) {
        return question.replaceAll("\\s+", "");
    }
}
