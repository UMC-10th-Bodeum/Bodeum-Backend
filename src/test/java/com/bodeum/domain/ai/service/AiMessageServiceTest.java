package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiWarningType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.repository.UserAgreementRepository;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiMessageServiceTest {

    @Mock AiChatRoomRepository aiChatRoomRepository;
    @Mock UserAgreementRepository userAgreementRepository;
    @Mock UserRepository userRepository;
    @Mock AiDocumentRetriever documentRetriever;
    @Mock AiAnswerGenerator answerGenerator;
    @Mock AiExternalAnswerProvider externalAnswerProvider;
    @Mock AiMessagePersistenceService persistenceService;
    @Mock AiSourceReviewRepository aiSourceReviewRepository;
    @Mock AiRequestGuard requestGuard;
    @Mock AiReferenceDocumentResolver referenceDocumentResolver;

    private AiMessageService service;
    private AiChatRoom chatRoom;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AiMessageService(
                aiChatRoomRepository, userAgreementRepository, userRepository,
                documentRetriever, answerGenerator, externalAnswerProvider,
                persistenceService, aiSourceReviewRepository, requestGuard,
                referenceDocumentResolver);
        user = User.createSocialUser(SocialProvider.KAKAO, "provider-id", "a@b.com", "보호자");
        chatRoom = AiChatRoom.create(user);
        lenient().when(userAgreementRepository.findByUserId(1L))
                .thenReturn(Optional.of(UserAgreement.create(user, true, true, true)));
        lenient().when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));
        lenient().when(userRepository.findAiProfileById(1L)).thenReturn(Optional.of(user));
        lenient().when(userRepository.findAiDisabilityProfileById(1L))
                .thenReturn(Optional.of(user));
        lenient().when(externalAnswerProvider.search(any(), any()))
                .thenReturn(ExternalAiAnswer.empty());
        lenient().when(referenceDocumentResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsQuestionWhenAiTermsAreNotAgreed() {
        when(userAgreementRepository.findByUserId(1L))
                .thenReturn(Optional.of(UserAgreement.create(user, true, true, false)));

        assertThatThrownBy(() -> service.createMessage(1L, "복지 센터 알려줘"))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_TERMS_NOT_AGREED);

        verify(documentRetriever, never()).retrieve(any(), any());
    }

    @Test
    void rejectsQuestionWhenAiChatRoomDoesNotExist() {
        when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createMessage(1L, "복지 센터 알려줘"))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND);

        verify(documentRetriever, never()).retrieve(any(), any());
    }

    @Test
    void doesNotCallOpenAiWhenNoReferenceDocumentExists() {
        when(documentRetriever.retrieve(eq("김치찌개 레시피 알려줘"), any())).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessage(
                eq(chatRoom), eq("관련 정보를 찾을 수 없습니다."), eq(false), eq(List.of())))
                .thenReturn(saved);

        var result = service.createMessage(1L, "김치찌개 레시피 알려줘");

        assertThat(result.aiMessage().content()).isEqualTo("관련 정보를 찾을 수 없습니다.");
        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.NO_EVIDENCE);
        assertThat(result.aiMessage().sources()).isEmpty();
        verify(answerGenerator, never()).generate(any(), any(), any());
    }

    @Test
    void returnsLinkGuidanceWhenExternalSearchHasNoCitation() {
        String question = "수원시 복지기관 알려줘";
        AiReferenceDocument externalSource = new AiReferenceDocument(
                "SITE-20",
                "전국장애인부모연대 수원시 복지기관 검색 결과",
                AiResponseSourceType.SITE,
                20L,
                "수원시 복지기관 검색",
                "https://www.bumo.or.kr/bbs/board.php?bo_table=B09&sido=경기도&sigu=수원시",
                null
        );
        when(documentRetriever.retrieve(eq(question), any())).thenReturn(List.of());
        when(externalAnswerProvider.search(eq(question), any())).thenReturn(
                ExternalAiAnswer.linkGuidance(
                        "수원시에서 확인 가능한 복지기관 정보입니다.",
                        List.of(externalSource)));
        AiMessage saved = savedAiMessage("수원시에서 확인 가능한 복지기관 정보입니다.");
        when(persistenceService.saveAiMessage(
                chatRoom,
                "수원시에서 확인 가능한 복지기관 정보입니다.",
                false,
                List.of(externalSource)
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().content())
                .isEqualTo("수원시에서 확인 가능한 복지기관 정보입니다.");
        assertThat(result.aiMessage().sources()).hasSize(1);
        assertThat(result.aiMessage().sources().getFirst().sourceType())
                .isEqualTo(AiResponseSourceType.SITE);
        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.LINK_GUIDANCE);
        assertThat(result.aiMessage().warning()).isNull();
        verify(answerGenerator, never()).generate(any(), any(), any());
    }

    @Test
    void returnsNoEvidenceWhenGeneratedAnswerHasNoValidCitation() {
        String question = "지원 제도를 알려줘";
        AiReferenceDocument retrievedSource = new AiReferenceDocument(
                "INFO-1-0",
                "지원 제도 안내",
                AiResponseSourceType.INFO,
                1L,
                "지원 제도",
                "https://example.com/info/1",
                Instant.parse("2026-07-01T00:00:00Z")
        );
        when(documentRetriever.retrieve(eq(question), any()))
                .thenReturn(List.of(retrievedSource));
        when(answerGenerator.generate(eq(question), any(), eq(List.of(retrievedSource))))
                .thenReturn(new GeneratedAiAnswer("근거가 검증되지 않은 답변", List.of("UNKNOWN")));
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessage(
                chatRoom, "관련 정보를 찾을 수 없습니다.", false, List.of()))
                .thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.NO_EVIDENCE);
        assertThat(result.aiMessage().content()).isEqualTo("관련 정보를 찾을 수 없습니다.");
        assertThat(result.aiMessage().sources()).isEmpty();
        assertThat(result.aiMessage().warning()).isNull();
    }

    @Test
    void returnsOnlyValidatedCitedSourcesAndWarning() {
        Instant updatedAt = Instant.parse("2026-07-01T00:00:00Z");
        AiReferenceDocument source = new AiReferenceDocument(
                "DOC-1", "복지로에서 지원금을 확인할 수 있습니다.",
                AiResponseSourceType.SITE, 10L, "복지로", "https://www.bokjiro.go.kr", updatedAt);
        when(documentRetriever.retrieve(eq("지원금 확인 사이트 알려줘"), any()))
                .thenReturn(List.of(source));
        when(answerGenerator.generate(eq("지원금 확인 사이트 알려줘"), any(), eq(List.of(source))))
                .thenReturn(new GeneratedAiAnswer("복지로에서 확인할 수 있습니다.", List.of("DOC-1")));
        when(aiSourceReviewRepository.existsConfirmedIncorrectBySources(
                java.util.Set.of(new AiSourceKey(AiResponseSourceType.SITE, 10L))
        )).thenReturn(true);
        AiMessage saved = savedAiMessage("복지로에서 확인할 수 있습니다.");
        when(persistenceService.saveAiMessage(
                chatRoom, "복지로에서 확인할 수 있습니다.", true, List.of(source)))
                .thenReturn(saved);

        var result = service.createMessage(1L, "지원금 확인 사이트 알려줘");

        assertThat(result.aiMessage().sources()).hasSize(1);
        assertThat(result.aiMessage().sources().getFirst().sourceUrl())
                .isEqualTo("https://www.bokjiro.go.kr");
        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().warning().type()).isEqualTo(AiWarningType.INCORRECT_SOURCE);
        assertThat(result.aiMessage().warning().message()).contains("오류 피드백");
    }

    private AiMessage savedAiMessage(String content) {
        AiMessage message = mock(AiMessage.class);
        when(message.getId()).thenReturn(12L);
        when(message.getSenderType()).thenReturn(SenderType.AI);
        when(message.getContent()).thenReturn(content);
        when(message.getCreatedAt()).thenReturn(Instant.parse("2026-07-03T06:30:03Z"));
        return message;
    }
}
