package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.infrastructure.generation.AiPromptFormatter;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiExternalSourceRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.client.RestClient;

class OpenAiExternalAnswerProviderTest {

    @Test
    void detectsExplicitNoEvidenceMarker() {
        assertThat(OpenAiExternalAnswerProvider.isNoEvidenceAnswer("[[NO_EVIDENCE]]"))
                .isTrue();
    }

    @Test
    void detectsNaturalLanguageNoEvidenceAnswer() {
        assertThat(OpenAiExternalAnswerProvider.isNoEvidenceAnswer(
                "허용된 사이트에서 김치찌개 레시피를 찾지 못했습니다."))
                .isTrue();
    }

    @Test
    void acceptsGroundedAnswer() {
        assertThat(OpenAiExternalAnswerProvider.isNoEvidenceAnswer(
                "한국장애인부모회에서 장애인 가족 지원 사업 정보를 확인할 수 있습니다."))
                .isFalse();
    }

    @Test
    void separatesSystemInstructionsFromUserInput() throws IOException {
        ClassPathResource promptResource = new ClassPathResource(
                "prompts/ai-external-search-system-prompt.txt"
        );
        String systemPrompt = promptResource.getContentAsString(StandardCharsets.UTF_8);
        AiPromptFormatter promptFormatter = mock(AiPromptFormatter.class);
        when(promptFormatter.formatProfile(any())).thenReturn("[사용자 맞춤 정보]");
        OpenAiExternalAnswerProvider provider = new OpenAiExternalAnswerProvider(
                mock(AiExternalSourceRepository.class),
                mock(AiExternalDocumentPersistenceService.class),
                RestClient.builder(),
                "test-key",
                "test-model",
                1200,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                promptResource,
                promptFormatter
        );

        Map<String, Object> body = provider.requestBody(
                "사용자 질문",
                List.of("사용자 질문", "공식 제도명 검색 질의"),
                mock(AiUserProfile.class),
                List.of("example.com")
        );

        assertThat(body.get("instructions")).isEqualTo(systemPrompt);
        assertThat((String) body.get("input"))
                .contains(
                        "[사용자 맞춤 정보]",
                        "[검색 질의 힌트]",
                        "공식 제도명 검색 질의",
                        "[사용자 질문]",
                        "사용자 질문"
                )
                .doesNotContain(systemPrompt);
    }
}
