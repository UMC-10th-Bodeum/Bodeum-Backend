package com.bodeum.domain.ai.infrastructure.config;

import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
public class AiInfrastructureFallbackConfiguration {

    @Bean
    AiDocumentRetriever emptyAiDocumentRetriever() {
        return (question, profile) -> List.of();
    }

    @Bean
    AiAnswerGenerator unavailableAiAnswerGenerator() {
        return (question, profile, documents) -> {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED);
        };
    }

    @Bean
    AiExternalAnswerProvider emptyAiExternalAnswerProvider() {
        return (question, profile) -> ExternalAiAnswer.empty();
    }
}
