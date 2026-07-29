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
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
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
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiMessageServiceTest {

    @Mock AiChatRoomRepository aiChatRoomRepository;
    @Mock AiMessageRepository aiMessageRepository;
    @Mock UserRepository userRepository;
    @Mock RegionRepository regionRepository;
    @Mock AiDocumentRetriever documentRetriever;
    @Mock AiAnswerGenerator answerGenerator;
    @Mock AiExternalAnswerProvider externalAnswerProvider;
    @Mock AiMessagePersistenceService persistenceService;
    @Mock AiMessageFailureService failureService;
    @Mock AiSourceReviewRepository aiSourceReviewRepository;
    @Mock AiRequestGuard requestGuard;
    @Mock AiReferenceDocumentResolver referenceDocumentResolver;
    @Mock AiStarterQuestionRouter starterQuestionRouter;

    private AiMessageService service;
    private AiChatRoom chatRoom;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AiMessageService(
                aiChatRoomRepository, aiMessageRepository, userRepository, regionRepository,
                documentRetriever, answerGenerator, externalAnswerProvider,
                persistenceService, failureService, aiSourceReviewRepository, requestGuard,
                referenceDocumentResolver, starterQuestionRouter);
        user = User.createSocialUser(SocialProvider.KAKAO, "provider-id", "a@b.com", "보호자");
        chatRoom = AiChatRoom.create(user);
        lenient().when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));
        lenient().when(userRepository.findAiProfileById(1L)).thenReturn(Optional.of(user));
        lenient().when(userRepository.findAiDisabilityProfileById(1L))
                .thenReturn(Optional.of(user));
        lenient().when(externalAnswerProvider.search(any(), any()))
                .thenReturn(ExternalAiAnswer.empty());
        lenient().when(referenceDocumentResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(starterQuestionRouter.route(any(), any()))
                .thenReturn(Optional.empty());
        AiMessage userMessage = mock(AiMessage.class);
        lenient().when(userMessage.getId()).thenReturn(11L);
        lenient().when(persistenceService.saveProcessingUserMessage(eq(chatRoom), any()))
                .thenReturn(userMessage);
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
    void marksUserMessageFailedWhenAnswerGenerationFails() {
        String question = "AI 실패 테스트 질문";
        when(documentRetriever.retrieve(eq(question), any()))
                .thenThrow(new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));

        assertThatThrownBy(() -> service.createMessage(1L, question))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_RESPONSE_FAILED);

        verify(failureService).markFailed(11L);
    }

    @Test
    void doesNotCallOpenAiWhenNoReferenceDocumentExists() {
        when(documentRetriever.retrieve(eq("김치찌개 레시피 알려줘"), any())).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                eq(11L), eq(chatRoom), eq("관련 정보를 찾을 수 없습니다."),
                eq(false), eq(AiAnswerStatus.NO_EVIDENCE), eq(List.of())))
                .thenReturn(saved);

        var result = service.createMessage(1L, "김치찌개 레시피 알려줘");

        assertThat(result.aiMessage().content()).isEqualTo("관련 정보를 찾을 수 없습니다.");
        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.NO_EVIDENCE);
        assertThat(result.aiMessage().sources()).isEmpty();
        verify(answerGenerator, never()).generate(any(), any(), any());
    }

    @Test
    void returnsRoutedStarterAnswerWithoutCallingOpenAi() {
        String question = AiStarterQuestionType.WELFARE_SITES.getContent();
        AiReferenceDocument source = new AiReferenceDocument(
                "SITE-1",
                "복지로",
                AiResponseSourceType.SITE,
                1L,
                "복지로",
                "https://www.bokjiro.go.kr",
                null
        );
        when(starterQuestionRouter.route(
                eq(AiStarterQuestionType.WELFARE_SITES),
                any()
        )).thenReturn(Optional.of(
                AiStarterQuestionAnswer.answered("공식 복지 사이트 안내", List.of(source))
        ));
        AiMessage saved = savedAiMessage("공식 복지 사이트 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "공식 복지 사이트 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().sources()).hasSize(1);
        verify(documentRetriever, never()).retrieve(any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any());
    }

    @Test
    void returnsRegionRequiredWhenLocalCenterQuestionHasNoRegion() {
        String question = AiStarterQuestionType.LOCAL_REHAB_CENTERS.getContent();
        String regionRequiredMessage = "확인할 시·도와 시·군·구를 알려주세요.";
        when(starterQuestionRouter.route(
                eq(AiStarterQuestionType.LOCAL_REHAB_CENTERS),
                any()
        ))
                .thenReturn(Optional.of(
                        AiStarterQuestionAnswer.regionRequired(regionRequiredMessage)
                ));
        AiMessage saved = savedAiMessage(regionRequiredMessage);
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                regionRequiredMessage,
                false,
                AiAnswerStatus.REGION_REQUIRED,
                List.of()
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus())
                .isEqualTo(AiAnswerStatus.REGION_REQUIRED);
        assertThat(result.aiMessage().sources()).isEmpty();
        verify(documentRetriever, never()).retrieve(any(), any());
        verify(externalAnswerProvider, never()).search(any(), any());
    }

    @Test
    void prioritizesExplicitRegionInRehabCenterQuestion() {
        Region region = Region.create("경기도", "수원시");
        when(regionRepository.findMentionedInQuestion(
                eq("경기도 수원시 재활센터를 추천해줘"),
                any()
        )).thenReturn(List.of(region));

        AiReferenceDocument source = new AiReferenceDocument(
                "INFO-1",
                "수원시 재활센터",
                AiResponseSourceType.INFO,
                1L,
                "수원시 재활센터",
                "https://example.com/info/1",
                null
        );
        when(starterQuestionRouter.route(
                eq(AiStarterQuestionType.LOCAL_REHAB_CENTERS),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        profile.region().equals("경기도 수원시")
                                && profile.regionLevel1().equals("경기도")
                                && profile.regionLevel2().equals("수원시"))
        )).thenReturn(Optional.of(
                AiStarterQuestionAnswer.answered(
                        "경기도 수원시 재활센터 안내",
                        List.of(source)
                )
        ));
        AiMessage saved = savedAiMessage("경기도 수원시 재활센터 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "경기도 수원시 재활센터 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        var result = service.createMessage(
                1L,
                "경기도 수원시 재활센터를 추천해줘."
        );

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().sources()).hasSize(1);
        verify(aiMessageRepository, never())
                .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        any(),
                        any()
                );
        verify(documentRetriever, never()).retrieve(any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any());
    }

    @Test
    void usesGeneralRagWhenExplicitRegionQuestionHasAdditionalRehabCondition() {
        String question = "경기도 수원시 자폐스펙트럼 재활센터를 추천해줘";
        Region region = Region.create("경기도", "수원시");
        when(regionRepository.findMentionedInQuestion(
                eq(question),
                any()
        )).thenReturn(List.of(region));
        when(documentRetriever.retrieve(eq(question), any()))
                .thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                eq(11L),
                eq(chatRoom),
                eq("관련 정보를 찾을 수 없습니다."),
                eq(false),
                eq(AiAnswerStatus.NO_EVIDENCE),
                eq(List.of())
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus())
                .isEqualTo(AiAnswerStatus.NO_EVIDENCE);
        verify(starterQuestionRouter, never()).route(
                eq(AiStarterQuestionType.LOCAL_REHAB_CENTERS),
                any()
        );
        verify(documentRetriever).retrieve(eq(question), any());
    }

    @Test
    void routesRegionOnlyFollowUpToLocalRehabCenters() {
        AiMessage previousAiMessage = mock(AiMessage.class);
        when(previousAiMessage.getAiAnswerStatus())
                .thenReturn(AiAnswerStatus.REGION_REQUIRED);
        when(aiMessageRepository
                .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoom.getId(),
                        SenderType.AI
                ))
                .thenReturn(Optional.of(previousAiMessage));
        Region region = Region.create("경기도", "수원시");
        when(regionRepository.findByFullName("경기도 수원시"))
                .thenReturn(Optional.of(region));

        AiReferenceDocument source = new AiReferenceDocument(
                "INFO-1",
                "수원시 재활센터",
                AiResponseSourceType.INFO,
                1L,
                "수원시 재활센터",
                "https://example.com/info/1",
                null
        );
        when(starterQuestionRouter.route(
                eq(AiStarterQuestionType.LOCAL_REHAB_CENTERS),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        profile.region().equals("경기도 수원시")
                                && profile.regionLevel1().equals("경기도")
                                && profile.regionLevel2().equals("수원시"))
        )).thenReturn(Optional.of(
                AiStarterQuestionAnswer.answered(
                        "경기도 수원시 재활센터 안내",
                        List.of(source)
                )
        ));
        AiMessage saved = savedAiMessage("경기도 수원시 재활센터 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "경기도 수원시 재활센터 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        var result = service.createMessage(1L, "  경기도   수원시  ");

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().sources()).hasSize(1);
        verify(documentRetriever, never()).retrieve(any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any());
    }

    @Test
    void limitsMedicalSupportFallbackSearchToBokjiro() {
        String question = AiStarterQuestionType.CHILD_MEDICAL_SUPPORT.getContent();
        when(documentRetriever.retrieve(
                eq(question
                        + "\n중앙부처복지서비스 장애아동 의료비 지원 대상 선정 기준 신청 방법"),
                any()
        )).thenReturn(List.of());
        when(externalAnswerProvider.searchWithinDomains(
                eq(question),
                any(),
                eq(Set.of("bokjiro.go.kr"))
        )).thenReturn(ExternalAiAnswer.empty());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "관련 정보를 찾을 수 없습니다.",
                false,
                AiAnswerStatus.NO_EVIDENCE,
                List.of()
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.NO_EVIDENCE);
        verify(externalAnswerProvider).searchWithinDomains(
                eq(question),
                any(),
                eq(Set.of("bokjiro.go.kr"))
        );
        verify(externalAnswerProvider, never()).search(any(), any());
    }

    @Test
    void prioritizesOfficialVoucherPagesForVoucherFallback() {
        String question = AiStarterQuestionType.VOUCHER_APPLICATION.getContent();
        when(documentRetriever.retrieve(
                eq(question
                        + "\n발달재활서비스 바우처 지원 대상 서비스 내용 신청 방법 제공기관"),
                any()
        )).thenReturn(List.of());
        List<String> preferredUrls = List.of(
                "https://www.bokjiro.go.kr/ssis-tbu/ssis-tbu/twataa/wlfareInfo/"
                        + "moveTWAT52011M.do?wlfareInfoId=WLF00003195",
                "https://www.socialservice.or.kr:444/",
                "https://www.bokjiro.go.kr/ssis-tbu/twofa/followStep/"
                        + "selectFollowStepTwoaa.do"
        );
        when(externalAnswerProvider.searchWithinSources(
                eq(question),
                any(),
                eq(Set.of("bokjiro.go.kr", "socialservice.or.kr")),
                eq(preferredUrls)
        )).thenReturn(ExternalAiAnswer.empty());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "관련 정보를 찾을 수 없습니다.",
                false,
                AiAnswerStatus.NO_EVIDENCE,
                List.of()
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(externalAnswerProvider).searchWithinSources(
                eq(question),
                any(),
                eq(Set.of("bokjiro.go.kr", "socialservice.or.kr")),
                eq(preferredUrls)
        );
        verify(externalAnswerProvider, never()).search(any(), any());
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
        when(persistenceService.saveAiMessageAndComplete(
                11L, chatRoom,
                "수원시에서 확인 가능한 복지기관 정보입니다.",
                false,
                AiAnswerStatus.LINK_GUIDANCE,
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
    void searchesExternalSourcesWhenGeneratedAnswerHasNoValidCitation() {
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
        AiReferenceDocument externalSource = new AiReferenceDocument(
                "SITE-20",
                "한국장애인부모회 공공후견지원사업 안내",
                AiResponseSourceType.SITE,
                20L,
                "공공후견지원사업 안내",
                "https://www.kpat.or.kr/guardianship",
                null
        );
        when(externalAnswerProvider.search(eq(question), any())).thenReturn(
                new ExternalAiAnswer(
                        "한국장애인부모회 공공후견지원사업 정보입니다.",
                        List.of(externalSource)));
        AiMessage saved = savedAiMessage("한국장애인부모회 공공후견지원사업 정보입니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "한국장애인부모회 공공후견지원사업 정보입니다.",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(externalSource)))
                .thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().content())
                .isEqualTo("한국장애인부모회 공공후견지원사업 정보입니다.");
        assertThat(result.aiMessage().sources()).hasSize(1);
        verify(externalAnswerProvider).search(eq(question), any());
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
        when(aiSourceReviewRepository.existsWarningRequiredBySources(
                java.util.Set.of(new AiSourceKey(AiResponseSourceType.SITE, 10L))
        )).thenReturn(true);
        AiMessage saved = savedAiMessage("복지로에서 확인할 수 있습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L, chatRoom, "복지로에서 확인할 수 있습니다.", true,
                AiAnswerStatus.ANSWERED, List.of(source)))
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
