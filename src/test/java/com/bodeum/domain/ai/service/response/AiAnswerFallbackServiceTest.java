package com.bodeum.domain.ai.service.response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.chat.AiMessagePersistenceService;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiAnswerFallbackServiceTest {

    @Test
    void supplementsOnlyMissingSiteDomainsForGeneralSiteList() {
        AiExternalAnswerProvider externalProvider = mock(AiExternalAnswerProvider.class);
        AiMessagePersistenceService persistenceService = mock(AiMessagePersistenceService.class);
        AiAnswerEvidenceService evidenceService = mock(AiAnswerEvidenceService.class);
        AiAnswerFallbackService service = new AiAnswerFallbackService(
                externalProvider, persistenceService, evidenceService,
                new AiAnswerResultNormalizer());
        List<AiReferenceDocument> internalSources = List.of(
                source(1L, "복지로", "https://www.bokjiro.go.kr/guide"),
                source(2L, "정부24", "https://www.gov.kr")
        );
        when(externalProvider.search(any(), any(), any(), any())).thenReturn(
                new ExternalAiAnswer("외부 사이트", List.of(
                        source(3L, "복지로 중복", "https://m.bokjiro.go.kr/page"),
                        source(4L, "보건복지부", "https://www.mohw.go.kr"),
                        source(5L, "국민건강보험", "https://www.nhis.or.kr"),
                        source(6L, "사회서비스 전자바우처", "https://socialservice.or.kr")
                )));

        List<AiReferenceDocument> result = service.supplementSiteSources(
                "공식 사이트 5개", List.of(),
                new AiUserProfile(null, null, null, null, List.of(), List.of(), null),
                AiSearchScope.NATIONWIDE, internalSources, 5);

        assertThat(result)
                .hasSize(5)
                .extracting(AiReferenceDocument::title)
                .containsExactly("복지로", "정부24", "보건복지부", "국민건강보험",
                        "사회서비스 전자바우처");
    }

    @Test
    void preservesCuratedSitesAndAddsOnlyNewExternalDomains() {
        AiExternalAnswerProvider externalProvider = mock(AiExternalAnswerProvider.class);
        AiMessagePersistenceService persistenceService = mock(AiMessagePersistenceService.class);
        AiAnswerEvidenceService evidenceService = mock(AiAnswerEvidenceService.class);
        AiAnswerFallbackService service = new AiAnswerFallbackService(
                externalProvider, persistenceService, evidenceService,
                new AiAnswerResultNormalizer());
        AiChatRoom chatRoom = mock(AiChatRoom.class);
        AiMessage userMessage = mock(AiMessage.class);
        when(userMessage.getId()).thenReturn(11L);
        AiMessage savedMessage = mock(AiMessage.class);
        when(savedMessage.getId()).thenReturn(12L);
        when(savedMessage.getSenderType()).thenReturn(SenderType.AI);
        when(savedMessage.getContent()).thenReturn("저장된 답변");
        when(persistenceService.saveAiMessageAndComplete(
                eq(11L), eq(chatRoom), any(), eq(false),
                eq(AiAnswerStatus.ANSWERED), any())).thenReturn(savedMessage);

        List<AiReferenceDocument> fixedSources = List.of(
                source(1L, "복지로", "https://bokjiro.go.kr"),
                source(2L, "발달장애인지원포털", "https://broso.or.kr"),
                source(3L, "사회서비스 전자바우처", "https://socialservice.or.kr"),
                source(4L, "경기도 장애인가족지원센터", "https://ggdf.co.kr"),
                source(5L, "정부24", "https://gov.kr")
        );
        AiStarterQuestionAnswer starterAnswer = AiStarterQuestionAnswer.answered(
                "네, 참고하면 좋을 공식 복지 사이트 5개를 추천드리겠습니다!\n\n"
                        + "**자주 확인하면 좋은 공식 복지 사이트**\n\n고정 사이트 목록",
                fixedSources);
        when(externalProvider.search(any(), any(), any(), any())).thenReturn(
                new ExternalAiAnswer("외부 사이트 목록", List.of(
                        source(6L, "복지로 중복", "https://www.bokjiro.go.kr/page"),
                        source(7L, "국민건강보험", "https://www.nhis.or.kr"),
                        source(8L, "보건복지부", "https://www.mohw.go.kr")
                )));

        service.saveStarterSiteAnswer(
                chatRoom, userMessage, "복지사이트 7개 알려줘", List.of(),
                new AiUserProfile(null, null, null, null, List.of(), List.of(), null),
                AiSearchScope.NATIONWIDE, starterAnswer, 7);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiReferenceDocument>> sourcesCaptor =
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(persistenceService).saveAiMessageAndComplete(
                eq(11L), eq(chatRoom), contentCaptor.capture(), eq(false),
                eq(AiAnswerStatus.ANSWERED), sourcesCaptor.capture());
        assertThat(sourcesCaptor.getValue())
                .hasSize(7)
                .extracting(AiReferenceDocument::title)
                .contains("국민건강보험", "보건복지부")
                .doesNotContain("복지로 중복");
        assertThat(contentCaptor.getValue())
                .contains("고정 사이트 목록", "추가로 확인한 공식 사이트")
                .contains("요청하신 개수에 맞춰 현재 보듬에서 확인 가능한 "
                        + "공식 사이트 7곳을 안내드립니다.")
                .satisfies(content -> assertThat(content.indexOf("요청하신 개수에 맞춰"))
                        .isLessThan(content.indexOf("자주 확인하면 좋은 공식 복지 사이트")))
                .doesNotContain("공식 복지 사이트 5개를 추천드리겠습니다");
    }

    private AiReferenceDocument source(Long id, String title, String url) {
        return new AiReferenceDocument(
                "SITE-" + id, title, AiResponseSourceType.SITE,
                id, title, url, null);
    }
}
