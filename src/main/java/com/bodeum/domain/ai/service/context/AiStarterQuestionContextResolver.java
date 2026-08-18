package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.question.AiCuratedAnswerType;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.question.AiStarterQuestionCatalog;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiQuestionContext;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.util.AiTextNormalizer;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 초기 추천 질문, 명시적 지역 재활센터 질문, 지역 후속 답변을 해석하여
 * 검색 범위와 결과 유형이 포함된 AiQuestionContext를 생성한다.
 */
@Component
@RequiredArgsConstructor
public class AiStarterQuestionContextResolver {

    private static final String AMBIGUOUS_REGION_MESSAGE_PREFIX =
            "확인할 지역이 여러 곳입니다. ";
    private static final Set<String> EXPLICIT_REGION_REHAB_QUESTIONS = Set.of(
            "재활센터추천해줘", "재활센터를추천해줘",
            "재활센터알려줘", "재활센터를알려줘");

    private final AiMessageRepository aiMessageRepository;
    private final RegionRepository regionRepository;

    /**
     * 초기 질문칩 또는 지역 후속 질문을 해석하여
     * 검색에 사용할 질문 문맥을 생성한다.
     */
    @Transactional(readOnly = true)
    public Optional<AiQuestionContext> resolve(
            Long chatRoomId,
            String content,
            AiUserProfile profile
    ) {
        // 질문에 지역이 직접 포함된 재활센터 추천 요청을 우선 처리
        Optional<Region> explicitRegion = resolveExplicitRehabRegion(content);
        if (explicitRegion.isPresent()) {
            return Optional.of(localRehabContext(profile, explicitRegion.get()));
        }

        // 고정 초기 질문칩과 일치하면 질문 유형에 맞는 문맥 생성
        Optional<AiCuratedAnswerType> curatedAnswerType =
                AiStarterQuestionCatalog.findAnswerType(content);
        if (curatedAnswerType.isPresent()) {
            return Optional.of(starterQuestionContext(
                    profile, profile, curatedAnswerType.get()));
        }

        // 직전 REGION_REQUIRED 응답에 대한 지역명 후속 답변 처리
        return resolveRegionFollowUp(chatRoomId, content)
                .map(region -> localRehabContext(profile, region));
    }

    /**
     * 사용자 원본 프로필과 검색용 프로필을 분리하여
     * 초기 질문 유형에 맞는 AiQuestionContext를 생성한다.
     */
    private AiQuestionContext starterQuestionContext(
            AiUserProfile userProfile,
            AiUserProfile searchProfile,
            AiCuratedAnswerType curatedAnswerType
    ) {
        return new AiQuestionContext(
                userProfile, searchProfile,
                Optional.of(curatedAnswerType), Optional.empty(), searchScope(curatedAnswerType),
                List.of(), null, null, null, List.of(), false, null, null,
                starterResultType(curatedAnswerType),
                false, false);
    }

    /**
     * 초기 질문 유형에 따라 응답 결과 형식을 결정한다.
     */
    private AiResultType starterResultType(AiCuratedAnswerType curatedAnswerType) {
        return switch (curatedAnswerType) {
            case WELFARE_SITES, AUTISM_INFO_SITES -> AiResultType.SITE_LIST;
            case LOCAL_REHAB_CENTERS -> AiResultType.RESOURCE_LIST;
            default -> AiResultType.DOCUMENT_ANSWER;
        };
    }

    /**
     * 초기 질문 유형별 검색 범위를 결정한다.
     */
    private AiSearchScope searchScope(AiCuratedAnswerType curatedAnswerType) {
        return switch (curatedAnswerType) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_ONLY;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION -> AiSearchScope.NATIONWIDE;
            default -> AiSearchScope.REGION_PRIORITY;
        };
    }

    /**
     * 특정 지역을 검색 프로필에 반영하여
     * 지역 재활센터 검색 문맥을 생성한다.
     */
    private AiQuestionContext localRehabContext(AiUserProfile profile, Region region) {
        AiUserProfile searchProfile = profile.withRegion(
                region.getFullName(), region.getRegionLevel1(), region.getRegionLevel2());
        return starterQuestionContext(
                profile, searchProfile, AiCuratedAnswerType.LOCAL_REHAB_CENTERS);
    }

    /**
     * "수원 재활센터 추천해줘"처럼 지역이 직접 포함된
     * 재활센터 추천 질문에서 지역을 추출한다.
     */
    private Optional<Region> resolveExplicitRehabRegion(String content) {
        String normalizedQuestion = normalizeQuestion(content);
        boolean rehabRecommendation = normalizedQuestion.contains("재활센터")
                && (normalizedQuestion.contains("추천")
                || normalizedQuestion.contains("알려"));
        if (!rehabRecommendation) {
            return Optional.empty();
        }
        String normalizedContent = AiTextNormalizer.normalizeQuestionSpacing(content);
        return regionRepository.findMentionedInQuestion(
                        normalizedContent, PageRequest.of(0, 1)).stream()
                .filter(region -> isGenericRehabQuestion(normalizedContent, region))
                .findFirst();
    }

    /**
     * 질문에서 지역명을 제거한 뒤,
     * 남은 내용이 고정 재활센터 추천 질문 패턴인지 확인한다.
     */
    private boolean isGenericRehabQuestion(String question, Region region) {
        String questionWithoutRegion = question.replace(region.getFullName(), " ")
                .trim().replaceFirst("^(에서|의|에|내)\\s*", "");
        String normalizedQuestion = normalizeQuestion(questionWithoutRegion)
                .replaceFirst("추천해주세요$", "추천해줘")
                .replaceFirst("알려주세요$", "알려줘");
        return EXPLICIT_REGION_REHAB_QUESTIONS.contains(normalizedQuestion);
    }

    /**
     * 직전 AI가 지역 입력을 요구한 상태라면,
     * 현재 사용자 답변을 지역명으로 해석한다.
     */
    private Optional<Region> resolveRegionFollowUp(Long chatRoomId, String content) {
        boolean awaitingRegion = aiMessageRepository
                .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.AI)
                // 일반 지역 모호성 안내는 REGION_REQUIRED 후속 입력 처리에서 제외
                .filter(message -> message.getContent() == null
                        || !message.getContent().startsWith(AMBIGUOUS_REGION_MESSAGE_PREFIX))
                .map(AiMessage::getAiAnswerStatus)
                .filter(status -> status == AiAnswerStatus.REGION_REQUIRED)
                .isPresent();
        if (!awaitingRegion) {
            return Optional.empty();
        }

        // "강남구예요", "강남구입니다" 등의 종결 표현을 제거
        String regionName = AiTextNormalizer.normalizeQuestionSpacing(content)
                .replaceFirst("(입니다|이에요|예요|이야|야)$", "").trim();
        return regionName.isEmpty() ? Optional.empty()
                : regionRepository.findByFullName(regionName);
    }

    private String normalizeQuestion(String content) {
        return AiTextNormalizer.removeWhitespace(
                AiTextNormalizer.normalizeQuestionSpacing(content));
    }
}
