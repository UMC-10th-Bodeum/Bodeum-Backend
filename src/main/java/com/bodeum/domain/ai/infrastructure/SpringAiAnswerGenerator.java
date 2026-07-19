package com.bodeum.domain.ai.infrastructure;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.model.AiReferenceDocument;
import com.bodeum.domain.ai.model.AiUserProfile;
import com.bodeum.domain.ai.model.GeneratedAiAnswer;
import com.bodeum.domain.ai.service.AiAnswerGenerator;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class SpringAiAnswerGenerator implements AiAnswerGenerator {

    private final ChatClient chatClient;
    private final AiPromptFormatter promptFormatter;

    public SpringAiAnswerGenerator(
            ChatClient.Builder builder,
            AiPromptFormatter promptFormatter,
            @Value("classpath:prompts/ai-rag-system-prompt.txt") Resource systemPromptResource
    ) {
        this.chatClient = builder.defaultSystem(readPrompt(systemPromptResource)).build();
        this.promptFormatter = promptFormatter;
    }

    @Override
    public GeneratedAiAnswer generate(
            String question,
            AiUserProfile profile,
            List<AiReferenceDocument> documents
    ) {
        String prompt = """
                [사용자 프로필]
                %s

                [참고자료]
                %s

                [사용자 질문]
                %s
                """.formatted(
                promptFormatter.formatProfile(profile),
                formatDocuments(documents),
                question
        );

        try {
            GeneratedAiAnswer answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(GeneratedAiAnswer.class, spec -> spec
                            .useProviderStructuredOutput()
                            .validateSchema());
            if (answer == null || answer.answer() == null || answer.answer().isBlank()) {
                throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED);
            }
            return answer;
        } catch (ProjectException e) {
            throw e;
        } catch (Exception e) {
            if (AiTimeoutDetector.isTimeout(e)) {
                throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT);
            }
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED);
        }
    }

    private String formatDocuments(List<AiReferenceDocument> documents) {
        return documents.stream()
                .map(document -> "[%s]\n%s".formatted(
                        document.documentKey(), document.content()))
                .reduce((left, right) -> left + "\n\n" + right)
                .orElse("");
    }

    private String readPrompt(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("AI RAG 시스템 프롬프트를 읽을 수 없습니다.", e);
        }
    }
}
