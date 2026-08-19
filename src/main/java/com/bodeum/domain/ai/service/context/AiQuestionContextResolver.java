package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.model.question.AiQuestionIntent;
import com.bodeum.domain.ai.model.question.AiCuratedAnswerResolver;
import com.bodeum.domain.ai.model.question.AiSafetyGuidanceResolver;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.question.AiCuratedAnswerType;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.context.AiQuestionContext;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import com.bodeum.domain.ai.util.AiTextNormalizer;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import com.bodeum.domain.region.entity.Region;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 사용자 질문과 이전 대화 문맥을 분석하여
 * 검색 범위, 결과 유형, 지역 및 검색 조건이 포함된 질문 문맥을 생성한다.
 */
@Component
public class AiQuestionContextResolver {

    private static final Pattern LOCAL_RESOURCE_PATTERN = Pattern.compile(
            "(학교|센터|기관|병원|의원|약국|복지관|시설|교육원|상담소|지원사업|지원서비스)");
    private static final Pattern RELATIVE_LOCAL_REGION_PATTERN = Pattern.compile(
            "(우리\\s*(지역|동네)|근처|주변)");
    private static final Pattern RESOURCE_LIST_REQUEST_PATTERN = Pattern.compile(
            "(알려줘|알려주세요|안내해줘|안내해주세요|추천해줘|추천해주세요|"
                    + "찾아줘|찾아주세요|목록|어디|어떤\\s*(곳|학교|기관|센터))");
    private static final Pattern RESOURCE_DETAIL_REQUEST_PATTERN = Pattern.compile(
            "(입학|신청|이용|접수|운영|비용|가격|자격|조건|절차|방법|시간|"
                    + "전화번호|주소|홈페이지|상세|자세히)");
    private static final Pattern CONTEXT_REFERENCE_PATTERN = Pattern.compile(
            "(그중|그\\s*(학교|센터|기관|곳|서비스|제도)|위\\s*(학교|센터|기관|곳|서비스|제도)"
                    + "|앞서|이전|방금|해당)");
    private static final Pattern SHORT_ADDITIONAL_RESULTS_PATTERN = Pattern.compile(
            "^(?:(?:좀|조금)?더(?:알려줘|알려주세요)?|추가로(?:알려줘|알려주세요)?)$");
    private static final Pattern EXPLICIT_ADDITIONAL_RESULTS_PATTERN = Pattern.compile(
            "(?:(?:앞에서|이전에|전에|앞서).*(?:말한|안내한|추천한|소개한).*"
                    + "(?:빼고|제외하고)"
                    + "|(?:아직)?(?:안내|말|소개|추천)하지않은.*"
                    + "(?:곳|기관|학교|센터|사이트|홈페이지|항목)"
                    + "|(?:새로운|새|다른).*(?:곳|기관|학교|센터|사이트|홈페이지|항목).*"
                    + "(?:더|알려|보여|추천|찾))");
    private static final Set<InfoSubCategory> SEARCHABLE_CATEGORIES = Set.of(
            InfoSubCategory.PRIMARY_CARE, InfoSubCategory.EMERGENCY_CLINIC,
            InfoSubCategory.THERAPY_REHAB, InfoSubCategory.WELFARE_CENTER,
            InfoSubCategory.FAMILY_SUPPORT, InfoSubCategory.PRIVATE_WELFARE,
            InfoSubCategory.NATIONAL_WELFARE, InfoSubCategory.LOCAL_WELFARE,
            InfoSubCategory.SPECIAL_SCHOOL, InfoSubCategory.SPECIAL_EDU_SUPPORT,
            InfoSubCategory.LIFELONG_EDU, InfoSubCategory.REALTIME_JOB,
            InfoSubCategory.STANDARD_WORKPLACE);

    private final AiQuestionIntentClassifier questionIntentClassifier;
    private final AiQuestionRegionResolver questionRegionResolver;
    public AiQuestionContextResolver(
            AiQuestionIntentClassifier questionIntentClassifier,
            AiQuestionRegionResolver questionRegionResolver
    ) {
        this.questionIntentClassifier = questionIntentClassifier;
        this.questionRegionResolver = questionRegionResolver;
    }

    public AiQuestionContext resolve(
            String content,
            AiUserProfile profile,
            AiConversationContext conversationContext
    ) {
        // 질문에 포함된 지역을 해석하고, 전국 단위 시설 검색 여부를 판단
        AiQuestionRegionResolver.RegionResolution originalRegionResolution =
                questionRegionResolver.resolve(content, profile);
        boolean nationwideResourceQuestion = isNationwideResourceQuestion(
                content, originalRegionResolution);

        // "수원은?"처럼 지역만 변경한 후속 질문을 이전 질문과 결합
        Optional<String> regionFollowUpQuestion = resolveRegionOnlyFollowUpQuestion(
                content, profile, conversationContext, originalRegionResolution);

        // 지역 후속 질문은 복원된 질문으로, 그 외에는 이전 대화 문맥을 포함해 질문 의도 분석
        AiQuestionAnalysis analysis;
        if (regionFollowUpQuestion.isPresent()) {
            String resolvedRegionQuestion = regionFollowUpQuestion.get();
            analysis = forceResolvedTarget(
                    questionIntentClassifier.analyze(
                            resolvedRegionQuestion, null, null, null, profile.region()),
                    resolvedRegionQuestion,
                    true,
                    false);
        } else {
            analysis = conversationContext.hasContext()
                    ? analyzeWithConversationContext(content, conversationContext, profile)
                    : questionIntentClassifier.analyze(
                            content, null, null, null, profile.region());
        }

        analysis = preserveImmediateListContextForShortAdditionalRequest(
                content, conversationContext, analysis);
        analysis = enforceExplicitAdditionalResultsRequest(
                content, conversationContext, analysis);

        // 사이트 목록 요청은 현재 질문을 명시적인 검색 대상으로 보정
        boolean analyzedSiteListRequest = analysis.siteListRequest()
                || analysis.intent() == AiQuestionIntent.WELFARE_SITES;
        if (analyzedSiteListRequest && regionFollowUpQuestion.isEmpty()) {
            analysis = forceResolvedTarget(
                    analysis, content, analysis.followUp(),
                    analysis.excludePreviousResults());
        }

        // 분석 결과를 기반으로 질문 유형, 결과 형식, 후속 질문 여부와 기본 검색 범위 결정
        AiQuestionIntent intent = analysis.intent();
        String resolvedQuestion = analysis.resolvedQuestion() == null
                ? content : analysis.resolvedQuestion();
        boolean followUp = regionFollowUpQuestion.isPresent()
                || analysis.referencesPreviousContext();
        boolean excludePreviousResults = followUp && analysis.excludePreviousResults();
        AiResultType resultType = resolveResultType(
                content, analysis, intent, analyzedSiteListRequest,
                conversationContext.immediatePreviousResolvedContext(),
                excludePreviousResults);
        AiSearchScope searchScope = resolveSearchScope(intent, analysis.searchScope());
        if (nationwideResourceQuestion) {
            searchScope = AiSearchScope.REGION_PRIORITY;
        }

        // 해석된 질문의 지역과 이전 대화 문맥을 병합해 구조화된 검색 조건 생성
        AiQuestionRegionResolver.RegionResolution regionResolution =
                questionRegionResolver.resolve(resolvedQuestion, profile);
        AiQuestionRegionResolver.RegionResolution contextRegionResolution =
                originalRegionResolution.isResolved()
                        ? originalRegionResolution
                        : regionResolution;
        AiResolvedContext resolvedContext = resolveStructuredContext(
                analysis.resolvedContext(),
                conversationContext.immediatePreviousResolvedContext(),
                contextRegionResolution, followUp,
                analysis.requestedResultCount());
        if (resolvedContext != null) {
            resolvedContext = resolvedContext.withResultType(resultType);
        }

        // 후속 질문이면 이전 검색 조건을 반영해 완전한 질문으로 다시 구성
        if (followUp && resolvedContext != null) {
            resolvedQuestion = resolvedContext.toResolvedQuestion(resolvedQuestion);
            regionResolution = questionRegionResolver.resolve(resolvedQuestion, profile);
        }

        // 지역 시설 질문은 명시된 지역 또는 사용자 프로필 지역을 기준으로 검색 범위 제한
        if (intent == AiQuestionIntent.NONE
                && regionResolution.isResolved()
                && isLocalResourceTarget(resolvedQuestion)) {
            searchScope = AiSearchScope.LOCAL_ONLY;
        }
        if (usesProfileRegionForLocalResource(content, resolvedQuestion, profile)) {
            searchScope = AiSearchScope.LOCAL_ONLY;
        }

        // 전국 단위 추천 사이트는 특정 지역으로 제한하지 않고 지역 우선 검색
        boolean nationwideStarterSiteRequest =
                AiCuratedAnswerResolver.resolve(intent)
                        .filter(type -> type == AiCuratedAnswerType.WELFARE_SITES
                                || type == AiCuratedAnswerType.AUTISM_INFO_SITES)
                        .isPresent();
        if (resultType == AiResultType.SITE_LIST && regionResolution.isResolved()
                && !nationwideStarterSiteRequest) {
            searchScope = AiSearchScope.LOCAL_ONLY;
        }
        if (nationwideStarterSiteRequest) {
            searchScope = AiSearchScope.REGION_PRIORITY;
        }

        // 검색 카테고리와 우선 지역을 반영해 실제 검색에 사용할 프로필 구성
        InfoSubCategory category = resultType == AiResultType.SITE_LIST
                ? null : resolveInfoSubCategory(resolvedQuestion, analysis.infoSubCategory());
        boolean needsClarification = analysis.needsClarification();
        String clarificationQuestion = analysis.clarificationQuestion();
        if (resultType == AiResultType.RESOURCE_LIST
                && category == null && !needsClarification) {
            needsClarification = true;
            clarificationQuestion = "어떤 종류의 기관을 찾으시나요? "
                    + "예: 재활센터, 특수학교, 장애인복지관, 가족지원센터";
        }
        AiQuestionRegionResolver.RegionResolution priorityRegion =
                resolveNationwideSearchPriorityRegion(
                        category, profile, conversationContext,
                        regionResolution, nationwideResourceQuestion);
        AiUserProfile searchProfile = (priorityRegion.isResolved()
                ? priorityRegion.toSearchProfile(profile) : profile)
                .withInfoSubCategory(category);

        return new AiQuestionContext(
                profile, searchProfile,
                AiCuratedAnswerResolver.resolve(intent),
                AiSafetyGuidanceResolver.resolve(intent),
                searchScope,
                intent == AiQuestionIntent.NONE ? analysis.retrievalQueries() : List.of(),
                analysis.requestedResultCount() == null && resolvedContext != null
                        ? resolvedContext.requestedResultCount()
                        : analysis.requestedResultCount(),
                resolvedQuestion, analysis.searchGoal(), analysis.requiredConcepts(),
                needsClarification, clarificationQuestion, resolvedContext,
                resultType,
                followUp, excludePreviousResults);
    }

    private AiQuestionAnalysis preserveImmediateListContextForShortAdditionalRequest(
            String content,
            AiConversationContext conversationContext,
            AiQuestionAnalysis analysis
    ) {
        AiResolvedContext previousContext = conversationContext
                .immediatePreviousResolvedContext();
        if (!isShortAdditionalRequest(content)
                || previousContext == null
                || !"목록".equals(previousContext.requestedInformation())) {
            return analysis;
        }
        String resolvedQuestion = previousContext.toResolvedQuestion(content);
        AiResultType previousResultType = previousListResultType(previousContext);
        return new AiQuestionAnalysis(
                AiQuestionIntent.NONE,
                analysis.searchScope(),
                List.of(resolvedQuestion),
                previousContext.requestedResultCount(),
                resolvedQuestion,
                resolveInfoSubCategory(resolvedQuestion, analysis.infoSubCategory()),
                analysis.searchGoal(),
                analysis.requiredConcepts(),
                false,
                null,
                previousContext,
                previousResultType == AiResultType.SITE_LIST,
                previousResultType == AiResultType.RESOURCE_LIST,
                true,
                true
        );
    }

    private AiResultType previousListResultType(AiResolvedContext previousContext) {
        if (previousContext.resultType() != null) {
            return previousContext.resultType();
        }
        String topic = AiTextNormalizer.removeWhitespace(previousContext.topic());
        return topic.contains("사이트") || topic.contains("홈페이지")
                ? AiResultType.SITE_LIST
                : AiResultType.RESOURCE_LIST;
    }

    private AiQuestionAnalysis enforceExplicitAdditionalResultsRequest(
            String content,
            AiConversationContext conversationContext,
            AiQuestionAnalysis analysis
    ) {
        AiResolvedContext previousContext = conversationContext
                .immediatePreviousResolvedContext();
        if (previousContext == null
                || !"목록".equals(previousContext.requestedInformation())
                || !isExplicitAdditionalResultsRequest(content)) {
            return analysis;
        }
        return analysis.withConversationContext(true, true);
    }

    private boolean isExplicitAdditionalResultsRequest(String content) {
        String normalized = AiTextNormalizer.removeWhitespace(
                AiTextNormalizer.normalizeQuestionSpacing(content));
        return SHORT_ADDITIONAL_RESULTS_PATTERN.matcher(normalized).matches()
                || EXPLICIT_ADDITIONAL_RESULTS_PATTERN.matcher(normalized).find();
    }

    private boolean isShortAdditionalRequest(String content) {
        return SHORT_ADDITIONAL_RESULTS_PATTERN.matcher(
                AiTextNormalizer.removeWhitespace(
                        AiTextNormalizer.normalizeQuestionSpacing(content)))
                .matches();
    }

    /**
     * 질문 의도와 분석 결과를 기반으로 최종 응답 결과 유형을 결정한다.
     */
    private AiResultType resolveResultType(
            String question,
            AiQuestionAnalysis analysis,
            AiQuestionIntent intent,
            boolean siteListRequest,
            AiResolvedContext previousContext,
            boolean excludePreviousResults
    ) {
        AiResultType currentType;
        if (intent == AiQuestionIntent.WELFARE_SITES
                || siteListRequest && isExplicitSiteListTarget(question)) {
            currentType = AiResultType.SITE_LIST;
        } else if (intent == AiQuestionIntent.LOCAL_REHAB_CENTERS) {
            currentType = AiResultType.RESOURCE_LIST;
        } else if (siteListRequest) {
            currentType = AiResultType.SITE_LIST;
        } else if (analysis.resourceListRequest()
                || isExplicitResourceListTarget(question)) {
            currentType = AiResultType.RESOURCE_LIST;
        } else {
            currentType = AiResultType.DOCUMENT_ANSWER;
        }

        if (!excludePreviousResults || previousContext == null
                || !"목록".equals(previousContext.requestedInformation())) {
            return currentType;
        }
        AiResultType previousType = previousListResultType(previousContext);
        return explicitlyChangesResultType(question, currentType, previousType)
                ? currentType : previousType;
    }

    private boolean explicitlyChangesResultType(
            String question,
            AiResultType currentType,
            AiResultType previousType
    ) {
        if (currentType == previousType) {
            return false;
        }
        if (currentType == AiResultType.SITE_LIST) {
            return isExplicitSiteListTarget(question);
        }
        if (currentType == AiResultType.RESOURCE_LIST) {
            return isLocalResourceTarget(question);
        }
        return false;
    }

    private boolean isExplicitSiteListTarget(String question) {
        String normalized = AiTextNormalizer.removeWhitespace(question);
        if (!normalized.contains("사이트") && !normalized.contains("홈페이지")) {
            return false;
        }
        return !normalized.contains("사이트있는")
                && !normalized.contains("사이트가있는")
                && !normalized.contains("사이트이있는")
                && !normalized.contains("홈페이지있는")
                && !normalized.contains("홈페이지가있는")
                && !normalized.contains("홈페이지이있는");
    }

    public boolean isLocalResourceTarget(String question) {
        return question != null && LOCAL_RESOURCE_PATTERN.matcher(question).find();
    }

    /**
     * 특수학교·재활센터처럼 코드로 카테고리를 확정할 수 있는 명백한 목록 질문은
     * LLM 분류가 실패하더라도 구조화된 기관 검색으로 처리한다.
     */
    private boolean isExplicitResourceListTarget(String question) {
        if (question == null || resolveInfoSubCategory(question, null) == null) {
            return false;
        }
        String normalized = AiTextNormalizer.normalizeQuestionSpacing(question);
        return RESOURCE_LIST_REQUEST_PATTERN.matcher(normalized).find()
                && !RESOURCE_DETAIL_REQUEST_PATTERN.matcher(normalized).find();
    }

    public InfoSubCategory resolveInfoSubCategory(String question) {
        return resolveInfoSubCategory(question, null);
    }

    /**
     * 최근 대화와 직전 문맥을 포함하여 후속 질문 의도를 분석
     */
    private AiQuestionAnalysis analyzeWithConversationContext(
            String content,
            AiConversationContext context,
            AiUserProfile profile
    ) {
        return questionIntentClassifier.analyze(
                content, context.recentConversation(),
                context.previousUserQuestion(), context.previousAiAnswer(),
                context.immediatePreviousResolvedContext(), profile.region());
    }

    /**
     * 지역명만 변경한 후속 질문을 이전 질문과 결합하여 완전한 질문으로 복원한다.
     */
    private Optional<String> resolveRegionOnlyFollowUpQuestion(
            String content,
            AiUserProfile profile,
            AiConversationContext context,
            AiQuestionRegionResolver.RegionResolution currentResolution
    ) {
        if (!context.hasContext() || context.immediatePreviousUserQuestion() == null
                || !questionRegionResolver.isRegionOnlyFollowUp(content, currentResolution)) {
            return Optional.empty();
        }
        String previousQuestion = context.immediatePreviousUserQuestion();
        return Optional.of(questionRegionResolver.replaceRegionInQuestion(
                previousQuestion,
                questionRegionResolver.resolve(previousQuestion, profile),
                currentResolution));
    }

    /**
     * 해석이 완료된 질문을 검색 쿼리에 우선 포함하고 추가 확인이 필요 없는 상태로 보정한다.
     */
    private AiQuestionAnalysis forceResolvedTarget(
            AiQuestionAnalysis analysis,
            String resolvedQuestion,
            boolean followUp,
            boolean excludePreviousResults
    ) {
        List<String> retrievalQueries = new ArrayList<>();
        retrievalQueries.add(resolvedQuestion);
        retrievalQueries.addAll(analysis.retrievalQueries());
        return new AiQuestionAnalysis(
                analysis.intent(), analysis.searchScope(), retrievalQueries,
                analysis.requestedResultCount(), resolvedQuestion,
                analysis.infoSubCategory(), analysis.searchGoal(), analysis.requiredConcepts(),
                false, null, analysis.resolvedContext(), analysis.siteListRequest(),
                analysis.resourceListRequest(), followUp, excludePreviousResults);
    }

    /**
     * 이전 대화 문맥과 현재 질문의 분석 결과를 병합하여
     * 지역, 결과 개수, 필터가 반영된 구조화 문맥을 생성한다.
     */
    private AiResolvedContext resolveStructuredContext(
            AiResolvedContext analyzedContext,
            AiResolvedContext previousContext,
            AiQuestionRegionResolver.RegionResolution regionResolution,
            boolean followUp,
            Integer requestedResultCount
    ) {
        // 후속 질문이면 이전 문맥에 현재 분석 결과를 병합
        AiResolvedContext resolved = followUp && previousContext != null
                ? previousContext.merge(analyzedContext) : analyzedContext;
        if (resolved == null) {
            if (requestedResultCount == null && !regionResolution.isResolved()) {
                return null;
            }
            resolved = new AiResolvedContext(
                    null, null, java.util.Map.of(), null, requestedResultCount);
        }
        if (requestedResultCount != null) {
            resolved = resolved.withRequestedResultCount(requestedResultCount);
        }
        if (regionResolution.isResolved()) {
            Region region = regionResolution.region();
            resolved = resolved.withRegion(
                    region == null ? regionResolution.regionLevel1() : region.getRegionLevel1(),
                    region == null ? null : region.getRegionLevel2());
        }
        return resolved.isEmpty() ? null : resolved;
    }

    /**
     * 특정 지역이 지정되지 않은 시설 검색 질문인지 판단한다.
     */
    private boolean isNationwideResourceQuestion(
            String question,
            AiQuestionRegionResolver.RegionResolution regionResolution
    ) {
        return isLocalResourceTarget(question)
                && !CONTEXT_REFERENCE_PATTERN.matcher(question).find()
                && !RELATIVE_LOCAL_REGION_PATTERN.matcher(question).find()
                && !regionResolution.isResolved()
                && resolveInfoSubCategory(question, null) != null;
    }

    /**
     * 지역 시설 질문의 상대적 지역 표현을 사용자 프로필 지역으로 해석해야 하는지 판단한다.
     */
    private boolean usesProfileRegionForLocalResource(
            String originalQuestion,
            String resolvedQuestion,
            AiUserProfile profile
    ) {
        return profile != null
                && originalQuestion != null
                && profile.region() != null
                && !profile.region().isBlank()
                && RELATIVE_LOCAL_REGION_PATTERN.matcher(originalQuestion).find()
                && (isLocalResourceTarget(originalQuestion)
                || isLocalResourceTarget(resolvedQuestion));
    }

    /**
     * 전국 시설 검색에서 이전 대화의 동일 카테고리 지역을 검색 우선 지역으로 유지한다.
     */
    private AiQuestionRegionResolver.RegionResolution resolveNationwideSearchPriorityRegion(
            InfoSubCategory category,
            AiUserProfile profile,
            AiConversationContext context,
            AiQuestionRegionResolver.RegionResolution currentResolution,
            boolean nationwideResourceQuestion
    ) {
        if (!nationwideResourceQuestion || currentResolution.isResolved()
                || category == null || !context.hasContext()) {
            return currentResolution;
        }
        String previousQuestion = context.immediatePreviousUserQuestion();
        if (previousQuestion == null
                || category != resolveInfoSubCategory(previousQuestion, null)) {
            return currentResolution;
        }
        AiQuestionRegionResolver.RegionResolution previousResolution =
                questionRegionResolver.resolve(previousQuestion, profile);
        return previousResolution.isResolved() ? previousResolution : currentResolution;
    }

    /**
     * 질문 의도와 분석 결과를 기반으로 검색 범위를 결정한다.
     */
    private AiSearchScope resolveSearchScope(
            AiQuestionIntent intent,
            AiSearchScope analyzedSearchScope
    ) {
        return switch (intent) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_ONLY;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION -> AiSearchScope.NATIONWIDE;
            default -> analyzedSearchScope == null ? AiSearchScope.REGION_PRIORITY : analyzedSearchScope;
        };
    }

    /**
     * 질문에 명시된 시설 유형을 검색 가능한 정보 하위 카테고리로 변환한다.
     */
    private InfoSubCategory resolveInfoSubCategory(
            String question,
            InfoSubCategory analyzedCategory
    ) {
        String normalized = AiTextNormalizer.removeWhitespace(question);
        if (normalized.contains("특수교육지원센터")) {
            return InfoSubCategory.SPECIAL_EDU_SUPPORT;
        }
        if (normalized.contains("특수학교")) {
            return InfoSubCategory.SPECIAL_SCHOOL;
        }
        if (normalized.contains("장애인평생교육")) {
            return InfoSubCategory.LIFELONG_EDU;
        }
        if (normalized.contains("응급의료기관")) {
            return InfoSubCategory.EMERGENCY_CLINIC;
        }
        if (normalized.contains("치료재활기관") || normalized.contains("재활센터")) {
            return InfoSubCategory.THERAPY_REHAB;
        }
        if (normalized.contains("장애인복지관")) {
            return InfoSubCategory.WELFARE_CENTER;
        }
        if (normalized.contains("장애인가족지원센터")) {
            return InfoSubCategory.FAMILY_SUPPORT;
        }
        if (normalized.contains("장애인표준사업장")) {
            return InfoSubCategory.STANDARD_WORKPLACE;
        }
        return analyzedCategory != null && SEARCHABLE_CATEGORIES.contains(analyzedCategory)
                ? analyzedCategory : null;
    }

}
