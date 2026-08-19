package com.bodeum.domain.ai.infrastructure.generation;

import com.bodeum.domain.ai.model.question.AiQuestionIntent;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.infrastructure.support.AiPromptTemplate;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
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
            @Value("${bodeum.ai.result.max-count:10}") int maxResultCount,
            @Value("${bodeum.ai.rag.max-query-count:3}") int maxQueryCount,
            @Value("${bodeum.ai.classifier.model:${spring.ai.openai.chat.options.model}}")
            String classifierModel,
            @Value("classpath:prompts/ai-question-intent-classifier-system-prompt.txt")
            Resource systemPromptResource,
            @Value("classpath:prompts/ai-query-expansion-system-prompt.txt")
            Resource queryExpansionPromptResource
    ) {
        validateMaxQueryCount(maxQueryCount);
        String queryExpansionPrompt = AiPromptTemplate.replaceRequiredPlaceholder(
                readPrompt(queryExpansionPromptResource),
                "{{maxQueryCount}}",
                Integer.toString(maxQueryCount)
        );
        String systemPrompt = AiPromptTemplate.replaceRequiredPlaceholder(
                readPrompt(systemPromptResource),
                "{{maxResultCount}}",
                Integer.toString(maxResultCount)
        )
                + "\n\n"
                + queryExpansionPrompt;
        this.chatClient = builder
                .defaultSystem(systemPrompt)
                .defaultOptions(OpenAiChatOptions.builder().model(classifierModel))
                .build();
    }

    @Override
    public AiQuestionAnalysis analyze(String question) {
        return analyze(question, null, null);
    }

    @Override
    public AiQuestionAnalysis analyze(
            String question,
            String previousUserQuestion,
            String previousAiAnswer
    ) {
        return analyze(question, previousUserQuestion, previousAiAnswer, null);
    }

    @Override
    public AiQuestionAnalysis analyze(
            String question,
            String previousUserQuestion,
            String previousAiAnswer,
            AiResolvedContext previousResolvedContext
    ) {
        return analyze(
                question, previousUserQuestion, previousAiAnswer,
                previousResolvedContext, null);
    }

    @Override
    public AiQuestionAnalysis analyze(
            String question,
            String previousUserQuestion,
            String previousAiAnswer,
            AiResolvedContext previousResolvedContext,
            String profileRegion
    ) {
        return analyze(
                question, null, previousUserQuestion, previousAiAnswer,
                previousResolvedContext, profileRegion);
    }

    @Override
    public AiQuestionAnalysis analyze(
            String question,
            String recentConversation,
            String previousUserQuestion,
            String previousAiAnswer,
            AiResolvedContext previousResolvedContext,
            String profileRegion
    ) {
        if (question == null || question.isBlank()) {
            return AiQuestionAnalysis.fallback();
        }

        try {
            StringBuilder userPrompt = new StringBuilder();
            if (recentConversation != null && !recentConversation.isBlank()) {
                userPrompt.append("[최근 대화]\n")
                        .append(recentConversation.trim())
                        .append("\n\n");
            } else if (previousUserQuestion != null && !previousUserQuestion.isBlank()
                    && previousAiAnswer != null && !previousAiAnswer.isBlank()) {
                userPrompt.append("[직전 사용자 질문]\n")
                        .append(previousUserQuestion.trim())
                        .append("\n\n[직전 AI 답변]\n")
                        .append(previousAiAnswer.trim())
                        .append("\n\n");
            }
            if (previousResolvedContext != null && !previousResolvedContext.isEmpty()) {
                userPrompt.append("[직전 구조화 문맥]\n")
                        .append(previousResolvedContext.toPromptText())
                        .append("\n\n");
            }
            if (profileRegion != null && !profileRegion.isBlank()) {
                userPrompt.append("[사용자 프로필 기본 지역]\n")
                        .append(profileRegion.trim())
                        .append("\n현재 질문이나 직전 대화에서 다른 지역을 지정하면 "
                                + "그 지역을 우선합니다.\n\n");
            }
            userPrompt.append("[분류할 현재 사용자 질문]\n").append(question.trim());
            ClassificationResult result = chatClient.prompt()
                    .user(userPrompt.toString())
                    .call()
                    .entity(ClassificationResult.class, spec -> spec
                            .useProviderStructuredOutput()
                            .validateSchema());
            String resolvedQuestion = result == null
                    || result.resolvedQuestion() == null
                    || result.resolvedQuestion().isBlank()
                    ? question
                    : result.resolvedQuestion().trim();
            AiQuestionAnalysis analysis = AiQuestionAnalysis.forQuestion(
                    resolvedQuestion,
                    result == null ? null : result.intent(),
                    result == null ? null : result.searchScope(),
                    result == null ? List.of() : result.retrievalQueries(),
                    result == null ? null : result.requestedResultCount(),
                    resolvedQuestion,
                    result != null && result.referencesPreviousContext(),
                    result == null ? null : result.infoSubCategory()
            ).withRetrievalPlan(
                    result == null ? null : result.searchGoal(),
                    result == null ? List.of() : result.requiredConcepts()
            ).withClarification(
                    result != null && result.needsClarification(),
                    result == null ? null : result.clarificationQuestion()
            ).withResolvedContext(result == null || result.resolvedContext() == null
                    ? null
                    : result.resolvedContext().toDomain())
                    .withSiteListRequest(result != null && result.siteListRequest())
                    .withResourceListRequest(
                            result != null && result.resourceListRequest())
                    .withConversationContext(
                            result != null && result.referencesPreviousContext(),
                            result != null && result.excludePreviousResults());
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
            List<String> retrievalQueries,
            Integer requestedResultCount,
            String resolvedQuestion,
            boolean referencesPreviousContext,
            boolean excludePreviousResults,
            InfoSubCategory infoSubCategory,
            String searchGoal,
            List<AiRequiredConcept> requiredConcepts,
            boolean needsClarification,
            String clarificationQuestion,
            ClassificationResolvedContext resolvedContext,
            boolean siteListRequest,
            boolean resourceListRequest
    ) {
    }

    /**
     * OpenAI Structured Outputs가 임의 키를 가진 Map 스키마를 허용하지 않으므로,
     * 질문 조건을 key/value 배열로 받은 뒤 도메인 문맥의 Map으로 변환한다.
     */
    record ClassificationResolvedContext(
            String topic,
            ClassificationRegion region,
            List<ClassificationFilter> filters,
            String requestedInformation,
            Integer requestedResultCount,
            AiResultType resultType
    ) {
        AiResolvedContext toDomain() {
            Map<String, String> resolvedFilters = new LinkedHashMap<>();
            if (filters != null) {
                filters.stream()
                        .filter(filter -> filter != null
                                && filter.name() != null
                                && filter.value() != null)
                        .forEach(filter -> resolvedFilters.put(
                                filter.name(), filter.value()));
            }
            return new AiResolvedContext(
                    topic,
                    region == null
                            ? null
                            : new AiResolvedContext.RegionContext(
                                    region.sido(), region.sigungu()),
                    resolvedFilters,
                    requestedInformation,
                    requestedResultCount,
                    resultType
            );
        }
    }

    record ClassificationRegion(String sido, String sigungu) {
    }

    record ClassificationFilter(String name, String value) {
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

    private void validateMaxQueryCount(int maxQueryCount) {
        if (maxQueryCount < 1 || maxQueryCount > 3) {
            throw new IllegalArgumentException(
                    "bodeum.ai.rag.max-query-count는 1 이상 3 이하여야 합니다.");
        }
    }
}
