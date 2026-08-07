package com.bodeum.domain.ai.infrastructure.generation;

import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.service.port.AiStarterQuestionClassifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
public class SpringAiStarterQuestionClassifier implements AiStarterQuestionClassifier {

    private final ChatClient chatClient;

    public SpringAiStarterQuestionClassifier(
            ChatClient.Builder builder,
            @Value("classpath:prompts/ai-starter-question-classifier-system-prompt.txt")
            Resource systemPromptResource
    ) {
        this.chatClient = builder.defaultSystem(readPrompt(systemPromptResource)).build();
    }

    @Override
    public Optional<AiStarterQuestionType> classify(String question) {
        if (question == null || question.isBlank()) {
            return Optional.empty();
        }

        try {
            ClassificationResult result = chatClient.prompt()
                    .user("[분류할 사용자 질문]\n" + question.trim())
                    .call()
                    .entity(ClassificationResult.class, spec -> spec
                            .useProviderStructuredOutput()
                            .validateSchema());
            if (result == null || result.intent() == null
                    || result.intent() == Intent.NONE) {
                log.info("[AI] 추천 질문 LLM 분류 결과: NONE");
                return Optional.empty();
            }

            AiStarterQuestionType type = AiStarterQuestionType.valueOf(result.intent().name());
            log.info("[AI] 추천 질문 LLM 분류 결과: {}", type);
            return Optional.of(type);
        } catch (Exception e) {
            log.warn("[AI] 추천 질문 LLM 분류 실패, 일반 RAG로 처리합니다.", e);
            return Optional.empty();
        }
    }

    enum Intent {
        WELFARE_SITES,
        LOCAL_REHAB_CENTERS,
        CHILD_MEDICAL_SUPPORT,
        DIAGNOSIS_FIRST_STEPS,
        VOUCHER_APPLICATION,
        NONE
    }

    record ClassificationResult(Intent intent) {
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "추천 질문 의도 분류 시스템 프롬프트를 읽을 수 없습니다.",
                    e
            );
        }
    }
}
