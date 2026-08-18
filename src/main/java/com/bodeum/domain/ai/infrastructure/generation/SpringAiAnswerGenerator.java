package com.bodeum.domain.ai.infrastructure.generation;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.support.AiPromptTemplate;
import com.bodeum.domain.ai.infrastructure.support.AiTimeoutDetector;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
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
            @Value("${bodeum.ai.result.max-count:10}") int maxResultCount,
            @Value("classpath:prompts/ai-rag-system-prompt.txt") Resource systemPromptResource
    ) {
        String systemPrompt = AiPromptTemplate.replaceRequiredPlaceholder(
                readPrompt(systemPromptResource),
                "{{maxResultCount}}",
                Integer.toString(maxResultCount)
        );
        this.chatClient = builder.defaultSystem(systemPrompt).build();
        this.promptFormatter = promptFormatter;
    }

    @Override
    public GeneratedAiAnswer generate(
            String question,
            AiUserProfile profile,
            List<AiReferenceDocument> documents
    ) {
        String prompt = """
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

        return generate(prompt);
    }

    @Override
    public GeneratedAiAnswer generate(
            String originalQuestion,
            String resolvedQuestion,
            AiResolvedContext resolvedContext,
            String searchRegion,
            AiUserProfile userProfile,
            List<AiReferenceDocument> documents
    ) {
        String prompt = """
                %s

                [참고자료]
                %s

                [현재 사용자 원문]
                %s

                [검색에 사용한 해석 질문]
                %s

                [구조화된 검색 문맥]
                %s

                [현재 요청에서 확정한 검색 지역]
                %s

                현재 사용자 원문에 명시된 지역·대상·조건을 최우선으로 답변하세요.
                해석 질문과 구조화 문맥은 검색 결과를 이해하기 위한 보조 정보이며,
                사용자 원문과 충돌하면 사용자 원문을 따르세요.
                """.formatted(
                promptFormatter.formatProfile(userProfile),
                formatDocuments(documents),
                originalQuestion,
                resolvedQuestion,
                resolvedContext == null ? "없음" : resolvedContext.toPromptText(),
                searchRegion == null || searchRegion.isBlank() ? "없음" : searchRegion
        );

        return generate(prompt);
    }

    private GeneratedAiAnswer generate(String prompt) {

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
                throw new ProjectException(AiErrorCode.AI_RESPONSE_TIMEOUT, e);
            }
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
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
