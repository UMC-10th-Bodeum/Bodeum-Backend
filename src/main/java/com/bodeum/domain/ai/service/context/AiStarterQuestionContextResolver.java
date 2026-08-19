package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.question.AiCuratedAnswerType;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.question.AiStarterQuestionCatalog;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiQuestionContext;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.util.AiTextNormalizer;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private static final Pattern RESULT_COUNT_PATTERN = Pattern.compile(
            "(?<!\\d)(\\d+)\\s*(?:개|곳|건)(?:을|를)?");

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
        Optional<CuratedRehabRequest> explicitRehabRequest =
                resolveExplicitRehabRequest(content);
        if (explicitRehabRequest.isPresent()) {
            CuratedRehabRequest request = explicitRehabRequest.get();
            return Optional.of(localRehabContext(
                    profile, request.region(), request.requestedResultCount()));
        }

        // 상대 지역 재활센터 질문은 수식어와 요청 개수가 포함되어도
        // LLM 분류를 거치지 않고 정보 탭 기관 검색으로 처리한다.
        Optional<CuratedRehabRequest> relativeRehabRequest =
                resolveRelativeRehabRequest(content);
        if (relativeRehabRequest.isPresent()) {
            return Optional.of(starterQuestionContext(
                    profile, profile, AiCuratedAnswerType.LOCAL_REHAB_CENTERS,
                    relativeRehabRequest.get().requestedResultCount()));
        }

        // 고정 초기 질문칩과 일치하면 질문 유형에 맞는 문맥 생성
        Optional<AiCuratedAnswerType> curatedAnswerType =
                AiStarterQuestionCatalog.findAnswerType(content);
        if (curatedAnswerType.isPresent()) {
            return Optional.of(starterQuestionContext(
                    profile, profile, curatedAnswerType.get(), null));
        }

        // 공식 사이트 질문은 지역명이 붙어도 전국 공통 선별 답변을 사용하며,
        // 명시한 개수만큼만 답변한다.
        Optional<CuratedSiteRequest> curatedSiteRequest =
                resolveParameterizedSiteQuestion(content);
        if (curatedSiteRequest.isPresent()) {
            CuratedSiteRequest request = curatedSiteRequest.get();
            return Optional.of(starterQuestionContext(
                    profile, profile, request.type(), request.requestedResultCount()));
        }

        // 직전 REGION_REQUIRED 응답에 대한 지역명 후속 답변 처리
        return resolveRegionFollowUp(chatRoomId, content)
                .map(region -> localRehabContext(profile, region, null));
    }

    /**
     * 사용자 원본 프로필과 검색용 프로필을 분리하여
     * 초기 질문 유형에 맞는 AiQuestionContext를 생성한다.
     */
    private AiQuestionContext starterQuestionContext(
            AiUserProfile userProfile,
            AiUserProfile searchProfile,
            AiCuratedAnswerType curatedAnswerType,
            Integer requestedResultCount
    ) {
        AiResolvedContext resolvedContext = curatedResolvedContext(
                searchProfile, curatedAnswerType, requestedResultCount);
        return new AiQuestionContext(
                userProfile, searchProfile,
                Optional.of(curatedAnswerType), Optional.empty(), searchScope(curatedAnswerType),
                List.of(), requestedResultCount, null,
                null, List.of(), false, null, resolvedContext,
                starterResultType(curatedAnswerType),
                false, false);
    }

    private AiResolvedContext curatedResolvedContext(
            AiUserProfile searchProfile,
            AiCuratedAnswerType type,
            Integer requestedResultCount
    ) {
        String topic = switch (type) {
            case WELFARE_SITES -> "공식 복지 사이트";
            case LOCAL_REHAB_CENTERS -> "재활센터";
            case CHILD_MEDICAL_SUPPORT -> "장애아동 의료비 지원";
            case DIAGNOSIS_FIRST_STEPS -> "장애 진단 후 해야 할 일";
            case VOUCHER_APPLICATION -> "발달재활서비스 바우처";
            case AUTISM_INFO_SITES -> "자폐스펙트럼 정보 사이트";
        };
        String requestedInformation = switch (type) {
            case WELFARE_SITES, LOCAL_REHAB_CENTERS, AUTISM_INFO_SITES -> "목록";
            case VOUCHER_APPLICATION -> "신청 방법";
            default -> "안내";
        };
        AiResolvedContext.RegionContext region = type == AiCuratedAnswerType.LOCAL_REHAB_CENTERS
                && searchProfile != null
                && (hasText(searchProfile.regionLevel1())
                || hasText(searchProfile.regionLevel2()))
                ? new AiResolvedContext.RegionContext(
                        searchProfile.regionLevel1(), searchProfile.regionLevel2())
                : null;
        return new AiResolvedContext(
                topic, region, Map.of(), requestedInformation, requestedResultCount,
                starterResultType(type));
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
    private AiQuestionContext localRehabContext(
            AiUserProfile profile,
            Region region,
            Integer requestedResultCount
    ) {
        AiUserProfile searchProfile = profile.withRegion(
                region.getFullName(), region.getRegionLevel1(), region.getRegionLevel2());
        return starterQuestionContext(
                profile, searchProfile, AiCuratedAnswerType.LOCAL_REHAB_CENTERS,
                requestedResultCount);
    }

    private Optional<CuratedSiteRequest> resolveParameterizedSiteQuestion(String content) {
        Integer requestedResultCount = requestedResultCount(content);
        String withoutCount = RESULT_COUNT_PATTERN.matcher(content).replaceAll(" ");
        Optional<AiCuratedAnswerType> directType = siteAnswerType(withoutCount);
        if (directType.isPresent()) {
            return Optional.of(new CuratedSiteRequest(
                    directType.get(), requestedResultCount));
        }

        String normalizedContent = AiTextNormalizer.normalizeQuestionSpacing(withoutCount);
        return regionRepository.findMentionedInQuestion(
                        normalizedContent, PageRequest.of(0, 1)).stream()
                .map(region -> removeRegionMention(normalizedContent, region))
                .map(this::siteAnswerType)
                .flatMap(Optional::stream)
                .findFirst()
                .map(type -> new CuratedSiteRequest(type, requestedResultCount));
    }

    private Optional<AiCuratedAnswerType> siteAnswerType(String question) {
        return AiStarterQuestionCatalog.findAnswerType(question)
                .filter(type -> type == AiCuratedAnswerType.WELFARE_SITES
                        || type == AiCuratedAnswerType.AUTISM_INFO_SITES);
    }

    private String removeRegionMention(String question, Region region) {
        String withoutRegion = question;
        for (String mention : new String[] {
                region.getFullName(), region.getRegionLevel1(), region.getRegionLevel2()}) {
            if (mention != null && !mention.isBlank()) {
                withoutRegion = withoutRegion.replace(mention, " ");
            }
        }
        return withoutRegion.trim().replaceFirst("^(에서|의|에|내)\\s*", "");
    }

    private Integer requestedResultCount(String content) {
        Matcher matcher = RESULT_COUNT_PATTERN.matcher(content);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * "수원 재활센터 추천해줘"처럼 지역이 직접 포함된
     * 재활센터 추천 질문에서 지역을 추출한다.
     */
    private Optional<CuratedRehabRequest> resolveExplicitRehabRequest(String content) {
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
                .findFirst()
                .map(region -> new CuratedRehabRequest(
                        region, requestedResultCount(content)));
    }

    private Optional<CuratedRehabRequest> resolveRelativeRehabRequest(String content) {
        String withoutCount = RESULT_COUNT_PATTERN.matcher(content).replaceAll(" ");
        String canonicalQuestion = withoutCount.replaceAll(
                "장애인\\s*재활센터", "재활센터");
        return AiStarterQuestionCatalog.findAnswerType(canonicalQuestion)
                .filter(type -> type == AiCuratedAnswerType.LOCAL_REHAB_CENTERS)
                .map(type -> new CuratedRehabRequest(
                        null, requestedResultCount(content)));
    }

    /**
     * 질문에서 지역명을 제거한 뒤,
     * 남은 내용이 고정 재활센터 추천 질문 패턴인지 확인한다.
     */
    private boolean isGenericRehabQuestion(String question, Region region) {
        String questionWithoutRegion = removeRegionMention(question, region);
        questionWithoutRegion = RESULT_COUNT_PATTERN.matcher(questionWithoutRegion)
                .replaceAll(" ")
                .replaceAll("장애인\\s*재활센터", "재활센터");
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CuratedSiteRequest(
            AiCuratedAnswerType type,
            Integer requestedResultCount
    ) {
    }

    private record CuratedRehabRequest(
            Region region,
            Integer requestedResultCount
    ) {
    }
}
