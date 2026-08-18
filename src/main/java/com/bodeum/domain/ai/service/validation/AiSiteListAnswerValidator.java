package com.bodeum.domain.ai.service.validation;

import com.bodeum.domain.ai.infrastructure.support.AiSiteDomainNormalizer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswerItem;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 사이트 목록 답변의 각 항목이 인용 근거와 대응하고
 * 동일 기관이 중복되지 않는지 검증한다.
 */
@Component
public class AiSiteListAnswerValidator {

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
}
