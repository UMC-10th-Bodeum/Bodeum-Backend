package com.bodeum.domain.ai.infrastructure.generation;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
public class SpringAiQuestionIntentClassifier implements AiQuestionIntentClassifier {

    private final ChatClient chatClient;

    public SpringAiQuestionIntentClassifier(
            ChatClient.Builder builder,
            @Value("classpath:prompts/ai-question-intent-classifier-system-prompt.txt")
            Resource systemPromptResource
    ) {
        this.chatClient = builder.defaultSystem(readPrompt(systemPromptResource)).build();
    }

    @Override
    public AiQuestionIntent classify(String question) {
        if (question == null || question.isBlank()) {
            return AiQuestionIntent.NONE;
        }

        try {
            ClassificationResult result = chatClient.prompt()
                    .user("[분류할 사용자 질문]\n" + question.trim())
                    .call()
                    .entity(ClassificationResult.class, spec -> spec
                            .useProviderStructuredOutput()
                            .validateSchema());
            AiQuestionIntent intent = result == null || result.intent() == null
                    ? AiQuestionIntent.NONE
                    : result.intent();
            log.info("[AI] 질문 LLM 의도 분류 결과: {}", intent);
            return intent;
        } catch (Exception e) {
            log.warn("[AI] 질문 LLM 의도 분류 실패, 일반 RAG로 처리합니다.", e);
            return AiQuestionIntent.NONE;
        }
    }

    record ClassificationResult(AiQuestionIntent intent) {
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "질문 의도 분류 시스템 프롬프트를 읽을 수 없습니다.",
                    e
            );
        }
    }
}
