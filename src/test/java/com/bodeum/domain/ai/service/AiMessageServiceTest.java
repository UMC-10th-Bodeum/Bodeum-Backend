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
import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.enums.AiWarningType;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.service.port.AiAnswerGenerator;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import com.bodeum.domain.ai.service.port.AiExternalAnswerProvider;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
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
    @Mock AiResponseSourceRepository aiResponseSourceRepository;
    @Mock AiRequestGuard requestGuard;
    @Mock AiReferenceDocumentResolver referenceDocumentResolver;
    @Mock AiStarterQuestionRouter starterQuestionRouter;
    @Mock AiQuestionIntentClassifier questionIntentClassifier;
    @Mock AiScrapInterestService scrapInterestService;

    private AiMessageService service;
    private AiChatRoom chatRoom;
    private User user;

    @BeforeEach
    void setUp() {
        service = new AiMessageService(
                aiChatRoomRepository, aiMessageRepository, userRepository, regionRepository,
                documentRetriever, answerGenerator, externalAnswerProvider,
                persistenceService, failureService, aiSourceReviewRepository,
                aiResponseSourceRepository, requestGuard,
                referenceDocumentResolver, starterQuestionRouter,
                questionIntentClassifier, scrapInterestService);
        user = User.createSocialUser(SocialProvider.KAKAO, "provider-id", "a@b.com", "보호자");
        chatRoom = AiChatRoom.create(user);
        lenient().when(aiChatRoomRepository.findByUserId(1L)).thenReturn(Optional.of(chatRoom));
        lenient().when(userRepository.findAiProfileById(1L)).thenReturn(Optional.of(user));
        lenient().when(userRepository.findAiDisabilityProfileById(1L))
                .thenReturn(Optional.of(user));
        lenient().when(externalAnswerProvider.search(any(), any(), any(), any()))
                .thenReturn(ExternalAiAnswer.empty());
        lenient().when(referenceDocumentResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(starterQuestionRouter.route(any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(questionIntentClassifier.analyze(any()))
                .thenReturn(AiQuestionAnalysis.fallback());
        lenient().when(scrapInterestService.findRecentInterests(1L))
                .thenReturn(AiScrapInterests.empty());
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

        verify(documentRetriever, never()).retrieve(any(), any(), any());
    }

    @Test
    void marksUserMessageFailedWhenAnswerGenerationFails() {
        String question = "AI 실패 테스트 질문";
        when(documentRetriever.retrieve(eq(question), any(), any()))
                .thenThrow(new ProjectException(AiErrorCode.AI_RESPONSE_FAILED));

        assertThatThrownBy(() -> service.createMessage(1L, question))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_RESPONSE_FAILED);

        verify(failureService).markFailed(11L);
    }

    @Test
    void doesNotCallOpenAiWhenNoReferenceDocumentExists() {
        when(documentRetriever.retrieve(eq("김치찌개 레시피 알려줘"), any(), any()))
                .thenReturn(List.of());
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
    void passesRecentScrapInterestsAsPersonalizationContext() {
        String question = "우리 지역 특수학교 알려줘";
        when(scrapInterestService.findRecentInterests(1L)).thenReturn(
                new AiScrapInterests(
                        List.of("수원시 특수교육 기관"),
                        List.of("특수학교 입학 안내"),
                        List.of("특수학교 정보 (게시판: INFORMATION_QUESTION)")
                )
        );
        when(documentRetriever.retrieve(
                eq(question),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        profile.scrappedInfoTitles().contains("수원시 특수교육 기관")
                                && profile.scrappedNewsTitles().contains("특수학교 입학 안내")
                                && profile.scrappedCommunityTopics()
                                .contains("특수학교 정보 (게시판: INFORMATION_QUESTION)"))
                , any()
        )).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                eq(11L), eq(chatRoom), eq("관련 정보를 찾을 수 없습니다."),
                eq(false), eq(AiAnswerStatus.NO_EVIDENCE), eq(List.of())
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(documentRetriever).retrieve(eq(question), any(), any());
    }

    @ParameterizedTest
    @CsvSource({
            "'우리 아이 증상 보고 자폐인지 진단해줘',MEDICAL_DIAGNOSIS,'진단해드릴 수 없습니다'",
            "'이 경우 소송하면 이길 수 있어?',LEGAL_ADVICE,'법률 자문을 제공해드릴 수 없습니다'",
            "'OO복지관이랑 XX복지관 중 어디가 더 좋아?',INSTITUTION_EVALUATION,'주관적으로 평가해드릴 수 없습니다'"
    })
    void replacesUnsafeQuestionsWithGuidance(
            String question,
            AiQuestionIntent intent,
            String expectedContent
    ) {
        when(questionIntentClassifier.analyze(question))
                .thenReturn(new AiQuestionAnalysis(intent, List.of()));
        when(persistenceService.saveAiMessageAndComplete(
                eq(11L),
                eq(chatRoom),
                any(),
                eq(false),
                eq(AiAnswerStatus.NO_EVIDENCE),
                eq(List.of())
        )).thenAnswer(invocation -> savedAiMessage(invocation.getArgument(2)));

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().content()).contains(expectedContent);
        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.NO_EVIDENCE);
        assertThat(result.aiMessage().sources()).isEmpty();
        verify(starterQuestionRouter, never()).route(any(), any());
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
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
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
    }

    @Test
    void fallsBackToGeneralFlowWhenStarterAnswerHasNoEvidence() {
        String question = AiStarterQuestionType.WELFARE_SITES.getContent();
        when(starterQuestionRouter.route(
                eq(AiStarterQuestionType.WELFARE_SITES),
                any()
        )).thenReturn(Optional.of(AiStarterQuestionAnswer.noEvidence()));
        when(documentRetriever.retrieve(eq(question), any(), any())).thenReturn(List.of());

        AiReferenceDocument source = new AiReferenceDocument(
                "SITE-1",
                "복지로",
                AiResponseSourceType.SITE,
                1L,
                "복지로",
                "https://www.bokjiro.go.kr",
                null
        );
        when(externalAnswerProvider.search(eq(question), any(), any(), any())).thenReturn(
                new ExternalAiAnswer("외부 검색 복지 사이트 안내", List.of(source))
        );
        AiMessage saved = savedAiMessage("외부 검색 복지 사이트 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "외부 검색 복지 사이트 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().content()).isEqualTo("외부 검색 복지 사이트 안내");
        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().sources()).hasSize(1);
        verify(documentRetriever).retrieve(eq(question), any(), any());
        verify(externalAnswerProvider).search(eq(question), any(), any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
    }

    @Test
    void routesSemanticallySimilarQuestionToReviewedStarterAnswer() {
        String question = "장애 진단을 받았는데 이제 뭘 먼저 해야 해?";
        when(questionIntentClassifier.analyze(question))
                .thenReturn(new AiQuestionAnalysis(
                        AiQuestionIntent.DIAGNOSIS_FIRST_STEPS,
                        List.of()
                ));

        AiReferenceDocument source = new AiReferenceDocument(
                "SITE-1",
                "장애 진단 이후 안내",
                AiResponseSourceType.SITE,
                1L,
                "장애 진단 이후 안내",
                "https://example.com/guide",
                null
        );
        when(starterQuestionRouter.route(
                eq(AiStarterQuestionType.DIAGNOSIS_FIRST_STEPS),
                any()
        )).thenReturn(Optional.of(
                AiStarterQuestionAnswer.answered("진단 이후 안내", List.of(source))
        ));
        AiMessage saved = savedAiMessage("진단 이후 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "진단 이후 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().content()).isEqualTo("진단 이후 안내");
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
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
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
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
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
    }

    @Test
    void usesGeneralRagWhenExplicitRegionQuestionHasAdditionalRehabCondition() {
        String question = "경기도 수원시 자폐스펙트럼 재활센터를 추천해줘";
        Region region = Region.create("경기도", "수원시");
        when(regionRepository.findMentionedInQuestion(
                eq(question),
                any()
        )).thenReturn(List.of(region));
        when(documentRetriever.retrieve(eq(question), any(), any()))
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
        verify(documentRetriever).retrieve(eq(question), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "  경기도   수원시  ",
            "경기도 수원시야.",
            "경기도 수원시입니다.",
            "경기도 수원시예요."
    })
    void routesRegionOnlyFollowUpToLocalRehabCenters(String regionAnswer) {
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

        var result = service.createMessage(1L, regionAnswer);

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().sources()).hasSize(1);
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(answerGenerator, never()).generate(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
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
        when(documentRetriever.retrieve(eq(question), any(), any())).thenReturn(List.of());
        when(externalAnswerProvider.search(eq(question), any(), any(), any())).thenReturn(
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
        when(documentRetriever.retrieve(eq(question), any(), any()))
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
        when(externalAnswerProvider.search(eq(question), any(), any(), any())).thenReturn(
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
        verify(externalAnswerProvider).search(eq(question), any(), any(), any());
        assertThat(result.aiMessage().warning()).isNull();
    }

    @Test
    void returnsOnlyValidatedCitedSourcesAndWarning() {
        Instant updatedAt = Instant.parse("2026-07-01T00:00:00Z");
        AiReferenceDocument source = new AiReferenceDocument(
                "DOC-1", "복지로에서 지원금을 확인할 수 있습니다.",
                AiResponseSourceType.SITE, 10L, "복지로", "https://www.bokjiro.go.kr", updatedAt);
        when(documentRetriever.retrieve(eq("지원금 확인 사이트 알려줘"), any(), any()))
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

    @Test
    void usesLlmNormalizedResultCountAndSkipsFixedStarterAnswer() {
        String question = "근처 장애인재활센터 열 개 알려줘";
        String searchQuestion = question + "\n요청 결과 개수: 10개";
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.LOCAL_REHAB_CENTERS,
                        AiSearchScope.LOCAL_RESOURCE,
                        List.of(),
                        10
                )
        );
        AiReferenceDocument source = referenceDocument("CENTER-1", 1L);
        when(documentRetriever.retrieve(eq(searchQuestion), any(), any()))
                .thenReturn(List.of(source));
        when(answerGenerator.generate(eq(question), any(), eq(List.of(source))))
                .thenReturn(new GeneratedAiAnswer(
                        "재활센터 안내",
                        List.of("CENTER-1")
                ));
        AiMessage saved = savedAiMessage("재활센터 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "재활센터 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(starterQuestionRouter, never()).route(any(), any());
        verify(documentRetriever).retrieve(
                eq(searchQuestion), any(), eq(AiSearchScope.LOCAL_RESOURCE));
    }

    @Test
    void keepsBaseTopicAndExcludesAllPreviouslyCitedSourcesForChainedMoreResults() {
        String question = "5개 더 알려줘";
        String previousQuestion = "근처 장애인재활센터 5개 알려줘";
        String resolvedQuestion = previousQuestion
                + "\n이전에 안내한 기관을 제외하고 " + question;
        AiMessage currentUserMessage = mock(AiMessage.class);
        AiMessage previousFollowUpMessage = mock(AiMessage.class);
        AiMessage previousUserMessage = mock(AiMessage.class);
        AiMessage previousAiMessage = mock(AiMessage.class);
        AiMessage baseAiMessage = mock(AiMessage.class);
        when(previousFollowUpMessage.getContent()).thenReturn("5개 더 알려줘");
        when(previousFollowUpMessage.getContextRootMessageId()).thenReturn(100L);
        when(previousUserMessage.getContent()).thenReturn(previousQuestion);
        when(previousAiMessage.getId()).thenReturn(20L);
        when(baseAiMessage.getId()).thenReturn(19L);
        when(aiMessageRepository.findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                any(), eq(SenderType.USER), any()))
                .thenReturn(List.of(
                        currentUserMessage,
                        previousFollowUpMessage,
                        previousUserMessage
                ));
        when(aiMessageRepository.findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                any(), eq(SenderType.AI), any()))
                .thenReturn(List.of(previousAiMessage, baseAiMessage));
        when(aiMessageRepository.findById(100L)).thenReturn(Optional.of(previousUserMessage));
        when(aiMessageRepository
                .findByChatRoomIdAndContextRootMessageIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        any(), eq(100L), eq(SenderType.AI)))
                .thenReturn(List.of(previousAiMessage, baseAiMessage));

        List<AiResponseSourceProjection> previousSources = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> {
                    AiResponseSourceProjection source = mock(AiResponseSourceProjection.class);
                    when(source.getSourceType()).thenReturn(AiResponseSourceType.INFO);
                    when(source.getSourceId()).thenReturn((long) index);
                    when(source.getSourceTitle()).thenReturn("기존센터-" + index);
                    return source;
                })
                .toList();
        when(aiResponseSourceRepository.findAllByMessageIds(List.of(20L, 19L)))
                .thenReturn(previousSources);
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        resolvedQuestion,
                        AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_RESOURCE,
                        List.of(),
                        5,
                        resolvedQuestion
                )
        );

        List<AiReferenceDocument> oldDocuments = new ArrayList<>();
        oldDocuments.add(new AiReferenceDocument(
                "ALIAS-OLD-1",
                "기존 센터 정보",
                AiResponseSourceType.SITE,
                999L,
                "[기관] 기존 센터-1",
                "https://different.example.com/center-1",
                Instant.parse("2026-07-01T00:00:00Z")
        ));
        oldDocuments.addAll(IntStream.rangeClosed(7, 10)
                .mapToObj(index -> referenceDocument("OLD-" + index, index))
                .toList());
        List<AiReferenceDocument> newDocuments = IntStream.rangeClosed(11, 15)
                .mapToObj(index -> referenceDocument("NEW-" + index, index))
                .toList();
        when(documentRetriever.retrieve(any(), any(), eq(AiSearchScope.LOCAL_RESOURCE)))
                .thenReturn(java.util.stream.Stream.concat(
                        oldDocuments.stream(), newDocuments.stream()).toList());
        when(answerGenerator.generate(
                eq(resolvedQuestion), any(), eq(newDocuments)))
                .thenReturn(new GeneratedAiAnswer(
                        "추가 재활센터 안내",
                        newDocuments.stream()
                                .map(AiReferenceDocument::documentKey)
                                .toList()
                ));
        AiMessage saved = savedAiMessage("추가 재활센터 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "추가 재활센터 안내",
                false,
                AiAnswerStatus.ANSWERED,
                newDocuments
        )).thenReturn(saved);

        service.createMessage(1L, question);

        ArgumentCaptor<String> searchQuestionCaptor = ArgumentCaptor.forClass(String.class);
        verify(documentRetriever).retrieve(
                searchQuestionCaptor.capture(), any(), eq(AiSearchScope.LOCAL_RESOURCE));
        assertThat(searchQuestionCaptor.getValue())
                .contains(previousQuestion)
                .contains("이전에 안내한 기관을 제외하고 5개 더 알려줘")
                .contains("검색 후보 개수: 10개")
                .contains("기존센터-1")
                .contains("기존센터-10");
        verify(answerGenerator).generate(
                eq(resolvedQuestion), any(), eq(newDocuments));
    }

    @Test
    void resolvesAmbiguousFollowUpQuestionFromPreviousConversation() {
        String question = "그중에서 공립만 알려줘";
        String previousQuestion = "수원시 특수학교를 알려줘";
        String previousResolvedQuestion = "경기도 수원시 특수학교를 알려줘";
        String previousAnswer = "아름학교, 자혜학교, 수원서광학교를 안내했습니다.";
        String resolvedQuestion = "수원시 특수학교 중 공립 학교만 알려줘";
        AiMessage currentUserMessage = mock(AiMessage.class);
        AiMessage previousUserMessage = mock(AiMessage.class);
        AiMessage previousAiMessage = mock(AiMessage.class);
        when(previousUserMessage.getResolvedQuestion()).thenReturn(previousResolvedQuestion);
        when(previousUserMessage.getId()).thenReturn(100L);
        when(previousAiMessage.getContent()).thenReturn(previousAnswer);
        when(aiMessageRepository.findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                any(), eq(SenderType.USER), any()))
                .thenReturn(List.of(currentUserMessage, previousUserMessage));
        when(aiMessageRepository
                .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        any(), eq(SenderType.AI)))
                .thenReturn(Optional.of(previousAiMessage));
        when(questionIntentClassifier.analyze(
                question,
                previousResolvedQuestion,
                previousAnswer
        )).thenReturn(AiQuestionAnalysis.forQuestion(
                resolvedQuestion,
                AiQuestionIntent.NONE,
                AiSearchScope.LOCAL_RESOURCE,
                List.of(),
                null,
                resolvedQuestion,
                true
        ));
        AiReferenceDocument source = referenceDocument("AREUM", 1L);
        when(documentRetriever.retrieve(
                eq(resolvedQuestion), any(), eq(AiSearchScope.LOCAL_RESOURCE)))
                .thenReturn(List.of(source));
        when(answerGenerator.generate(
                eq(resolvedQuestion), any(), eq(List.of(source))))
                .thenReturn(new GeneratedAiAnswer("공립 특수학교 안내", List.of("AREUM")));
        AiMessage saved = savedAiMessage("공립 특수학교 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "공립 특수학교 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(source)
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(questionIntentClassifier).analyze(
                question,
                previousResolvedQuestion,
                previousAnswer
        );
        verify(persistenceService).updateUserMessageContext(
                11L,
                resolvedQuestion,
                100L,
                100L
        );
        verify(answerGenerator).generate(
                eq(resolvedQuestion), any(), eq(List.of(source)));
    }

    @Test
    void startsNewContextRootWhenQuestionIsIndependentFromPreviousConversation() {
        String question = "장애인 활동지원 서비스를 알려줘";
        String previousQuestion = "수원시 특수학교를 알려줘";
        String previousAnswer = "아름학교, 자혜학교, 수원서광학교를 안내했습니다.";
        AiMessage currentUserMessage = mock(AiMessage.class);
        AiMessage previousUserMessage = mock(AiMessage.class);
        AiMessage previousAiMessage = mock(AiMessage.class);
        when(previousUserMessage.getContent()).thenReturn(previousQuestion);
        when(previousUserMessage.getId()).thenReturn(100L);
        when(previousUserMessage.getContextRootMessageId()).thenReturn(90L);
        when(previousAiMessage.getContent()).thenReturn(previousAnswer);
        when(aiMessageRepository.findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                any(), eq(SenderType.USER), any()))
                .thenReturn(List.of(currentUserMessage, previousUserMessage));
        when(aiMessageRepository.findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                any(), eq(SenderType.AI), any()))
                .thenReturn(List.of(previousAiMessage));
        when(questionIntentClassifier.analyze(
                question,
                previousQuestion,
                previousAnswer
        )).thenReturn(AiQuestionAnalysis.forQuestion(
                question,
                AiQuestionIntent.NONE,
                AiSearchScope.NATIONAL_POLICY,
                List.of(),
                null,
                question,
                false
        ));
        when(documentRetriever.retrieve(
                eq(question), any(), eq(AiSearchScope.NATIONAL_POLICY)))
                .thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                eq(11L), eq(chatRoom), eq("관련 정보를 찾을 수 없습니다."),
                eq(false), eq(AiAnswerStatus.NO_EVIDENCE), eq(List.of())))
                .thenReturn(saved);

        service.createMessage(1L, question);

        verify(persistenceService).updateUserMessageContext(
                11L,
                question,
                null,
                11L
        );
    }

    @Test
    void mergesDocumentsRetrievedByOriginalAndExpandedQueries() {
        String question = "장애아동 활동지원 서비스 신청 방법 알려줘";
        String broaderTargetQuery = "장애인 활동지원 서비스 신청 방법 알려줘";
        String localBroaderTargetQuery =
                "경기도 수원시 장애인 활동지원 서비스 신청 방법 알려줘";
        user.updateInterestRegion(List.of(), Region.create("경기도", "수원시"));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.NATIONAL_POLICY,
                        List.of("장애인 활동지원서비스 아동 신청 대상 신청 방법")
                )
        );

        AiReferenceDocument originalDocument = new AiReferenceDocument(
                "INFO-1-0",
                "장애아동 지원 정보",
                AiResponseSourceType.INFO,
                1L,
                "장애아동 지원",
                "https://example.com/info/1",
                Instant.parse("2026-07-01T00:00:00Z")
        );
        AiReferenceDocument nationalDocument = new AiReferenceDocument(
                "INFO-5724-0",
                "장애인활동지원 전국 공통 제도 안내",
                AiResponseSourceType.INFO,
                5724L,
                "장애인활동지원",
                "https://example.com/info/5724",
                Instant.parse("2026-07-01T00:00:00Z")
        );
        AiReferenceDocument localDocument = new AiReferenceDocument(
                "INFO-7183-0",
                "만 6세 이상 장애인 활동지원 대상자의 수원시 추가지원 안내",
                AiResponseSourceType.INFO,
                7183L,
                "장애인활동지원 수원시 추가지원",
                "https://example.com/info/7183",
                Instant.parse("2026-07-01T00:00:00Z")
        );
        when(documentRetriever.retrieve(eq(question), any(), any()))
                .thenReturn(List.of(originalDocument));
        when(documentRetriever.retrieve(eq(broaderTargetQuery), any(), any()))
                .thenReturn(List.of(nationalDocument));
        when(documentRetriever.retrieve(eq(localBroaderTargetQuery), any(), any()))
                .thenReturn(List.of(localDocument));
        when(answerGenerator.generate(
                eq(question),
                any(),
                eq(List.of(originalDocument, nationalDocument, localDocument))
        )).thenReturn(new GeneratedAiAnswer(
                "장애인활동지원과 수원시 추가지원 안내입니다.",
                List.of("INFO-5724-0", "INFO-7183-0")
        ));
        AiMessage saved = savedAiMessage("장애인활동지원과 수원시 추가지원 안내입니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "장애인활동지원과 수원시 추가지원 안내입니다.",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(nationalDocument, localDocument)
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().answerStatus()).isEqualTo(AiAnswerStatus.ANSWERED);
        assertThat(result.aiMessage().sources()).hasSize(2);
        verify(documentRetriever).retrieve(eq(question), any(), any());
        verify(documentRetriever).retrieve(eq(broaderTargetQuery), any(), any());
        verify(documentRetriever).retrieve(eq(localBroaderTargetQuery), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void roundRobinsOriginalAndExpandedResultsForChildDisabilityQuestion() {
        String question = "장애아동 활동지원 서비스를 알려줘";
        String broaderTargetQuery = "장애인 활동지원 서비스를 알려줘";
        String localBroaderTargetQuery =
                "경기도 수원시 장애인 활동지원 서비스를 알려줘";
        user.updateInterestRegion(List.of(), Region.create("경기도", "수원시"));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.NATIONAL_POLICY,
                        List.of()
                )
        );

        List<AiReferenceDocument> childDocuments = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> referenceDocument("CHILD-" + index, index))
                .toList();
        List<AiReferenceDocument> nationalDocuments = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> referenceDocument("NATIONAL-" + index, 100L + index))
                .toList();
        List<AiReferenceDocument> localDocuments = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> referenceDocument("LOCAL-" + index, 200L + index))
                .toList();
        when(documentRetriever.retrieve(eq(question), any(), any()))
                .thenReturn(childDocuments);
        when(documentRetriever.retrieve(eq(broaderTargetQuery), any(), any()))
                .thenReturn(nationalDocuments);
        when(documentRetriever.retrieve(eq(localBroaderTargetQuery), any(), any()))
                .thenReturn(localDocuments);
        when(answerGenerator.generate(eq(question), any(), any()))
                .thenReturn(new GeneratedAiAnswer(
                        "장애아동 활동지원 안내",
                        List.of("CHILD-4")
                ));
        AiMessage saved = savedAiMessage("장애아동 활동지원 안내");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "장애아동 활동지원 안내",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(childDocuments.get(3))
        )).thenReturn(saved);

        service.createMessage(1L, question);

        ArgumentCaptor<List<AiReferenceDocument>> documentsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerGenerator).generate(eq(question), any(), documentsCaptor.capture());
        assertThat(documentsCaptor.getValue()).containsExactly(
                childDocuments.get(0),
                nationalDocuments.get(0),
                localDocuments.get(0),
                childDocuments.get(1),
                nationalDocuments.get(1),
                localDocuments.get(1),
                childDocuments.get(2),
                nationalDocuments.get(2),
                localDocuments.get(2),
                childDocuments.get(3)
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void preservesOriginalDocumentsAndTopResultFromEachExpandedQuery() {
        String question = "수원시 특수학교를 알려줘";
        String expandedQuery1 = "수원시 특수교육 학교 현황";
        String expandedQuery2 = "경기도 수원시 특수학교 목록";
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_RESOURCE,
                        List.of(expandedQuery1, expandedQuery2)
                )
        );
        when(regionRepository.findMentionedInQuestion(any(), any()))
                .thenReturn(List.of());
        when(regionRepository.findAllByRegionLevel2OrderByIdAsc(any()))
                .thenReturn(List.of());

        List<AiReferenceDocument> originalDocuments = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> referenceDocument("ORIGINAL-" + index, index))
                .toList();
        List<AiReferenceDocument> expandedDocuments1 = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> referenceDocument("EXPANDED-A-" + index, 100L + index))
                .toList();
        List<AiReferenceDocument> expandedDocuments2 = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> referenceDocument("EXPANDED-B-" + index, 200L + index))
                .toList();
        when(documentRetriever.retrieve(eq(question), any(), any()))
                .thenReturn(originalDocuments);
        when(documentRetriever.retrieve(eq(expandedQuery1), any(), any()))
                .thenReturn(expandedDocuments1);
        when(documentRetriever.retrieve(eq(expandedQuery2), any(), any()))
                .thenReturn(expandedDocuments2);
        when(answerGenerator.generate(eq(question), any(), any()))
                .thenReturn(new GeneratedAiAnswer(
                        "원문 검색 결과를 사용한 답변",
                        List.of("ORIGINAL-5")
                ));
        AiMessage saved = savedAiMessage("원문 검색 결과를 사용한 답변");
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                "원문 검색 결과를 사용한 답변",
                false,
                AiAnswerStatus.ANSWERED,
                List.of(originalDocuments.get(4))
        )).thenReturn(saved);

        service.createMessage(1L, question);

        ArgumentCaptor<List<AiReferenceDocument>> documentsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(answerGenerator).generate(
                eq(question),
                any(),
                documentsCaptor.capture()
        );
        assertThat(documentsCaptor.getValue()).hasSize(10);
        assertThat(documentsCaptor.getValue().subList(0, 5))
                .containsExactlyElementsOf(originalDocuments);
        assertThat(documentsCaptor.getValue())
                .contains(
                        originalDocuments.get(4),
                        expandedDocuments1.getFirst(),
                        expandedDocuments2.getFirst()
                );
    }

    @Test
    void passesExpandedQueriesToExternalSearchWhenInternalEvidenceIsMissing() {
        String question = "장애 아동 활동지원 서비스 신청 방법 알려줘";
        String broaderTargetQuery = "장애인 활동지원 서비스 신청 방법 알려줘";
        String localBroaderTargetQuery =
                "경기도 수원시 장애인 활동지원 서비스 신청 방법 알려줘";
        user.updateInterestRegion(List.of(), Region.create("경기도", "수원시"));
        List<String> analyzedQueries = List.of(
                question,
                "장애인 활동지원서비스 아동 신청 대상 신청 방법"
        );
        when(questionIntentClassifier.analyze(question)).thenReturn(
                new AiQuestionAnalysis(
                        AiQuestionIntent.NONE,
                        AiSearchScope.NATIONAL_POLICY,
                        analyzedQueries
                )
        );
        when(documentRetriever.retrieve(any(), any(), any())).thenReturn(List.of());
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

        verify(externalAnswerProvider).search(
                eq(question),
                eq(List.of(
                        broaderTargetQuery,
                        localBroaderTargetQuery,
                        question
                )),
                any(),
                any()
        );
    }

    @Test
    void usesExplicitQuestionRegionInsteadOfProfileRegionForLocalSearch() {
        String question = "부산시 재활센터를 추천해줘";
        Region busanRegion = Region.create("부산광역시", "해운대구");
        when(regionRepository.findMentionedInQuestion(any(), any()))
                .thenReturn(List.of());
        when(regionRepository.findFirstByRegionLevel1OrderByIdAsc("부산광역시"))
                .thenReturn(Optional.of(busanRegion));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                new AiQuestionAnalysis(
                        AiQuestionIntent.LOCAL_REHAB_CENTERS,
                        List.of()
                )
        );
        when(documentRetriever.retrieve(eq(question), any(),
                eq(AiSearchScope.LOCAL_RESOURCE))).thenReturn(List.of());
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

        verify(documentRetriever).retrieve(
                eq(question),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "부산광역시".equals(profile.region())
                                && "부산광역시".equals(profile.regionLevel1())
                                && profile.regionLevel2().isBlank()),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
        verify(externalAnswerProvider).search(
                eq(question),
                any(),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "부산광역시".equals(profile.region())),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
    }

    @Test
    void resolvesMetropolitanRegionWithoutAdministrativeSuffix() {
        String question = "부산 특수학교를 알려줘";
        Region busanRegion = Region.create("부산광역시", "해운대구");
        user.updateInterestRegion(List.of(), Region.create("경기도", "수원시"));
        when(regionRepository.findMentionedInQuestion(any(), any()))
                .thenReturn(List.of());
        when(regionRepository.findFirstByRegionLevel1OrderByIdAsc("부산광역시"))
                .thenReturn(Optional.of(busanRegion));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.GENERAL,
                        List.of()
                )
        );
        when(documentRetriever.retrieve(eq(question), any(),
                eq(AiSearchScope.LOCAL_RESOURCE))).thenReturn(List.of());
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

        verify(documentRetriever).retrieve(
                eq(question),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "부산광역시".equals(profile.region())
                                && "부산광역시".equals(profile.regionLevel1())
                                && profile.regionLevel2().isBlank()),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
        verify(externalAnswerProvider).search(
                eq(question),
                any(),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "부산광역시".equals(profile.region())
                                && "부산광역시".equals(profile.regionLevel1())),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
    }

    @Test
    void asksWhichGwangjuWhenRegionNameIsAmbiguous() {
        String question = "광주 특수학교를 알려줘";
        String message = "광주광역시와 경기도 광주시 중 어느 지역을 말씀하시나요? "
                + "원하시는 지역명을 알려주세요.";
        AiMessage saved = savedAiMessage(message);
        when(persistenceService.saveAiMessageAndComplete(
                11L,
                chatRoom,
                message,
                false,
                AiAnswerStatus.REGION_REQUIRED,
                List.of()
        )).thenReturn(saved);

        var result = service.createMessage(1L, question);

        assertThat(result.aiMessage().content()).isEqualTo(message);
        assertThat(result.aiMessage().answerStatus())
                .isEqualTo(AiAnswerStatus.REGION_REQUIRED);
        verify(documentRetriever, never()).retrieve(any(), any(), any());
        verify(externalAnswerProvider, never()).search(any(), any(), any(), any());
    }

    @Test
    void distinguishesGwangjuMetropolitanCityFromGyeonggiGwangju() {
        String metropolitanQuestion = "광주광역시 특수학교를 알려줘";
        Region metropolitanRegion = Region.create("광주광역시", "동구");
        when(regionRepository.findMentionedInQuestion(any(), any()))
                .thenReturn(List.of());
        when(regionRepository.findFirstByRegionLevel1OrderByIdAsc("광주광역시"))
                .thenReturn(Optional.of(metropolitanRegion));
        when(questionIntentClassifier.analyze(metropolitanQuestion)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        metropolitanQuestion,
                        AiQuestionIntent.NONE,
                        AiSearchScope.GENERAL,
                        List.of()
                )
        );
        when(documentRetriever.retrieve(eq(metropolitanQuestion), any(),
                eq(AiSearchScope.LOCAL_RESOURCE))).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L, chatRoom, "관련 정보를 찾을 수 없습니다.", false,
                AiAnswerStatus.NO_EVIDENCE, List.of()
        )).thenReturn(saved);

        service.createMessage(1L, metropolitanQuestion);

        verify(documentRetriever).retrieve(
                eq(metropolitanQuestion),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "광주광역시".equals(profile.regionLevel1())
                                && profile.regionLevel2().isBlank()),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
    }

    @Test
    void resolvesGyeonggiGwangjuAlias() {
        String question = "경기 광주 특수학교를 알려줘";
        Region gyeonggiGwangju = Region.create("경기도", "광주시");
        when(regionRepository.findByFullName("경기도 광주시"))
                .thenReturn(Optional.of(gyeonggiGwangju));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.GENERAL,
                        List.of()
                )
        );
        when(documentRetriever.retrieve(eq(question), any(),
                eq(AiSearchScope.LOCAL_RESOURCE))).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L, chatRoom, "관련 정보를 찾을 수 없습니다.", false,
                AiAnswerStatus.NO_EVIDENCE, List.of()
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(documentRetriever).retrieve(
                eq(question),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "경기도 광주시".equals(profile.region())
                                && "경기도".equals(profile.regionLevel1())
                                && "광주시".equals(profile.regionLevel2())),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
    }

    @Test
    void removesProfileRegionFromGeneralSearchAndAnswerContext() {
        String question = "특수학교를 알려줘";
        user.updateInterestRegion(List.of(), Region.create("경기도", "수원시"));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.GENERAL,
                        List.of()
                )
        );
        when(documentRetriever.retrieve(eq(question), any(),
                eq(AiSearchScope.GENERAL))).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L, chatRoom, "관련 정보를 찾을 수 없습니다.", false,
                AiAnswerStatus.NO_EVIDENCE, List.of()
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(documentRetriever).retrieve(
                eq(question),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        profile.region().isBlank()
                                && profile.regionLevel1().isBlank()
                                && profile.regionLevel2().isBlank()),
                eq(AiSearchScope.GENERAL)
        );
        verify(externalAnswerProvider).search(
                eq(question),
                any(),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        profile.region().isBlank()
                                && profile.regionLevel1().isBlank()
                                && profile.regionLevel2().isBlank()),
                eq(AiSearchScope.GENERAL)
        );
    }

    @Test
    void replacesRelativeLocalExpressionWithProfileRegionForRetrieval() {
        String question = "우리 지역 특수학교를 알려줘";
        String contextualizedQuestion = "경기도 수원시 특수학교를 알려줘";
        user.updateInterestRegion(List.of(), Region.create("경기도", "수원시"));
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_RESOURCE,
                        List.of()
                )
        );
        when(regionRepository.findMentionedInQuestion(any(), any()))
                .thenReturn(List.of());
        when(documentRetriever.retrieve(eq(contextualizedQuestion), any(),
                eq(AiSearchScope.LOCAL_RESOURCE))).thenReturn(List.of());
        AiMessage saved = savedAiMessage("관련 정보를 찾을 수 없습니다.");
        when(persistenceService.saveAiMessageAndComplete(
                11L, chatRoom, "관련 정보를 찾을 수 없습니다.", false,
                AiAnswerStatus.NO_EVIDENCE, List.of()
        )).thenReturn(saved);

        service.createMessage(1L, question);

        verify(documentRetriever).retrieve(
                eq(contextualizedQuestion),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "경기도 수원시".equals(profile.region())),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
        verify(externalAnswerProvider).search(
                eq(contextualizedQuestion),
                eq(List.of(contextualizedQuestion)),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "경기도 수원시".equals(profile.region())),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
    }

    @Test
    void resolvesRegionLevel2AfterMetropolitanAlias() {
        String question = "서울시 강남구 특수학교 알려줘";
        Region seoulRegion = Region.create("서울특별시", "강남구");
        Region busanRegion = Region.create("부산광역시", "강남구");
        when(regionRepository.findMentionedInQuestion(any(), any()))
                .thenReturn(List.of());
        when(regionRepository.findFirstByRegionLevel1OrderByIdAsc("서울특별시"))
                .thenReturn(Optional.of(seoulRegion));
        when(regionRepository.findAllByRegionLevel2OrderByIdAsc(any()))
                .thenAnswer(invocation -> "강남구".equals(invocation.getArgument(0))
                        ? List.of(seoulRegion, busanRegion)
                        : List.of());
        when(questionIntentClassifier.analyze(question)).thenReturn(
                AiQuestionAnalysis.forQuestion(
                        question,
                        AiQuestionIntent.NONE,
                        AiSearchScope.LOCAL_RESOURCE,
                        List.of()
                )
        );
        when(documentRetriever.retrieve(eq(question), any(),
                eq(AiSearchScope.LOCAL_RESOURCE))).thenReturn(List.of());
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

        verify(documentRetriever).retrieve(
                eq(question),
                org.mockito.ArgumentMatchers.argThat(profile ->
                        "서울특별시 강남구".equals(profile.region())
                                && "서울특별시".equals(profile.regionLevel1())
                                && "강남구".equals(profile.regionLevel2())),
                eq(AiSearchScope.LOCAL_RESOURCE)
        );
    }

    private AiMessage savedAiMessage(String content) {
        AiMessage message = mock(AiMessage.class);
        when(message.getId()).thenReturn(12L);
        when(message.getSenderType()).thenReturn(SenderType.AI);
        when(message.getContent()).thenReturn(content);
        when(message.getCreatedAt()).thenReturn(Instant.parse("2026-07-03T06:30:03Z"));
        return message;
    }

    private AiReferenceDocument referenceDocument(String documentKey, long sourceId) {
        return new AiReferenceDocument(
                documentKey,
                documentKey + " 내용",
                AiResponseSourceType.INFO,
                sourceId,
                documentKey,
                "https://example.com/" + sourceId,
                Instant.parse("2026-07-01T00:00:00Z")
        );
    }
}
