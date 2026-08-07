package com.bodeum.domain.ai.infrastructure.generation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class AiResponseStylePromptTest {

    private static final String[] PROMPT_PATHS = {
            "prompts/ai-rag-system-prompt.txt",
            "prompts/ai-external-search-system-prompt.txt"
    };

    @Test
    void appliesSharedResponseStyleToRagAndExternalSearch() throws IOException {
        for (String promptPath : PROMPT_PATHS) {
            String prompt = readPrompt(promptPath);

            assertThat(prompt).contains(
                    "모든 답변은 정중한 존댓말과 표준어로 작성하세요.",
                    "질문에 대한 핵심 답변을 먼저 작성하세요.",
                    "핵심 안내 → 상세 내용 → 다음 행동 또는 공식 확인 경로",
                    "Markdown 제목과 번호 또는 글머리표 목록을 사용하세요.",
                    "감정형 이모티콘이나 이모지를 사용하지 마세요.",
                    "차분하고 신뢰감 있는 말투를 유지하세요.",
                    "질문과 관련 있을 때만 답변 마지막에 안내하세요."
            );
        }
    }

    private String readPrompt(String path) throws IOException {
        return new ClassPathResource(path)
                .getContentAsString(StandardCharsets.UTF_8);
    }
}
