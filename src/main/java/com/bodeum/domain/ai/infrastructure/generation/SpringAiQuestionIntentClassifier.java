package com.bodeum.domain.ai.infrastructure.generation;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
            Resource systemPromptResource,
            @Value("classpath:prompts/ai-query-expansion-system-prompt.txt")
            Resource queryExpansionPromptResource
    ) {
        String systemPrompt = readPrompt(systemPromptResource)
                + "\n\n"
                + readPrompt(queryExpansionPromptResource);
        this.chatClient = builder.defaultSystem(systemPrompt).build();
    }

    @Override
    public AiQuestionAnalysis analyze(String question) {
        if (question == null || question.isBlank()) {
            return AiQuestionAnalysis.fallback();
        }

        try {
            ClassificationResult result = chatClient.prompt()
                    .user("[분류할 사용자 질문]\n" + question.trim())
                    .call()
                    .entity(ClassificationResult.class, spec -> spec
                            .useProviderStructuredOutput()
                            .validateSchema());
            AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                    question,
                    result == null ? null : result.intent(),
                    result == null ? null : result.searchScope(),
                    result == null ? List.of() : result.retrievalQueries()
            );
            log.info(
                    "[AI] 질문 LLM 분석 결과: intent={}, searchScope={}, retrievalQueryCount={}",
                    analysis.intent(),
                    analysis.searchScope(),
                    analysis.retrievalQueries().size()
            );
            return analysis;
        } catch (Exception e) {
            log.warn("[AI] 질문 LLM 분석 실패, 사용자 원문으로 일반 RAG를 수행합니다.", e);
            return AiQuestionAnalysis.fallback(question);
        }
    }

    record ClassificationResult(
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> retrievalQueries
    ) {
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
