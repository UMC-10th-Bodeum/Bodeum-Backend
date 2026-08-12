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
                    "번호 목록의 각 항목 사이에는 빈 줄을 한 줄 넣어",
                    "각 학교·기관 이름을 Markdown 굵은 글씨(`**이름**`)로 표시하세요.",
                    "대상, 주소, 전화번호, 홈페이지 등 세부 정보는 글머리표 목록",
                    "개수를 지정했다면",
                    "한 번의 답변에서 최대 {{maxResultCount}}개까지",
                    "임의로 5개로 줄이지 마세요.",
                    "한 번에 최대 {{maxResultCount}}개까지 안내할 수 있습니다.",
                    "가능한 전체 항목을 요청한 경우에도 최대 {{maxResultCount}}개만 안내",
                    "1개 이상이고 요청 개수보다 적다면",
                    "관련성이 낮은 항목으로 개수를 채우지 마세요.",
                    "현재 확인 가능한 관련 항목은 M개입니다.",
                    "M은 실제로 확인된 항목 개수입니다.",
                    "이전에 안내한 항목을 제외하면, 추가로 확인 가능한 관련 항목은 M개입니다.",
                    "M은 실제로 확인된 추가 항목 개수입니다.",
                    "내부 검색 기준을 설명하지 마세요.",
                    "추가 항목이 1개 이상이면",
                    "추가로 확인되는 항목은 없습니다",
                    "관련 근거가 0개라면",
                    "이전 질문의 지역과 자원 유형을 유지",
                    "기존 기관은 다시 안내하지 마세요.",
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
