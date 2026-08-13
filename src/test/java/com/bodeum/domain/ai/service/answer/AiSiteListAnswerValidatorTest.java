package com.bodeum.domain.ai.service.answer;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswerItem;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiSiteListAnswerValidatorTest {

    private final AiSiteListAnswerValidator validator = new AiSiteListAnswerValidator();

    @Test
    void rejectsSiteListItemsUsingTheSameDomain() {
        GeneratedAiAnswer generated = new GeneratedAiAnswer(
                "복지 사이트 두 곳을 안내합니다.",
                List.of("SITE-1", "SITE-2"),
                List.of(
                        new GeneratedAiAnswerItem("복지로", "SITE-1"),
                        new GeneratedAiAnswerItem("복지로 안내", "SITE-2")
                )
        );
        List<AiReferenceDocument> documents = List.of(
                document("SITE-1", "https://www.bokjiro.go.kr/guide"),
                document("SITE-2", "https://m.bokjiro.go.kr/login")
        );

        assertThat(validator.isValid(generated, documents)).isFalse();
    }

    private AiReferenceDocument document(String key, String url) {
        return new AiReferenceDocument(
                key, key + " 내용", AiResponseSourceType.SITE, 1L, key, url, null);
    }
}
