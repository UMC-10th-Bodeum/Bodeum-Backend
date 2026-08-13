package com.bodeum.domain.ai.service.answer;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import org.springframework.stereotype.Component;

@Component
public class AiAnswerResultNormalizer {

    public GeneratedAiAnswer normalizeListedResultCount(
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
        return new GeneratedAiAnswer(
                answer + "\n\n" + countMessage,
                generated.citedDocumentKeys(),
                generated.answerItems()
        );
    }
}
