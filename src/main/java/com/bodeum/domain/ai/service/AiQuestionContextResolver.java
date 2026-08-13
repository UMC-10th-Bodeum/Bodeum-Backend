package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.enums.AiQuestionIntent;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.context.AiQuestionContext;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.service.port.AiQuestionIntentClassifier;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import com.bodeum.domain.region.entity.Region;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AiQuestionContextResolver {

    private static final Pattern LOCAL_RESOURCE_PATTERN = Pattern.compile(
            "(학교|센터|기관|병원|의원|약국|복지관|시설|교육원|상담소|지원사업|지원서비스)");
    private static final Pattern RELATIVE_LOCAL_REGION_PATTERN = Pattern.compile(
            "(우리\\s*(지역|동네)|근처|주변)");
    private static final Pattern CONTEXT_REFERENCE_PATTERN = Pattern.compile(
            "(그중|그\\s*(학교|센터|기관|곳|서비스|제도)|위\\s*(학교|센터|기관|곳|서비스|제도)"
                    + "|앞서|이전|방금|해당)");
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
    private final AiSiteListAnswerValidator siteListAnswerValidator;

    public AiQuestionContextResolver(
            AiQuestionIntentClassifier questionIntentClassifier,
            AiQuestionRegionResolver questionRegionResolver,
            AiSiteListAnswerValidator siteListAnswerValidator
    ) {
        this.questionIntentClassifier = questionIntentClassifier;
        this.questionRegionResolver = questionRegionResolver;
        this.siteListAnswerValidator = siteListAnswerValidator;
    }

    public AiQuestionContext resolve(
            String content,
            AiUserProfile profile,
            AiConversationContext conversationContext
    ) {
        AiQuestionRegionResolver.RegionResolution originalRegionResolution =
                questionRegionResolver.resolve(content, profile);
        boolean nationwideResourceQuestion = isNationwideResourceQuestion(
                content, originalRegionResolution);
        boolean selfContainedResourceQuestion = isSelfContainedResourceQuestion(
                content, content, originalRegionResolution);
        Optional<String> regionFollowUpQuestion = resolveRegionOnlyFollowUpQuestion(
                content, profile, conversationContext, originalRegionResolution);

        AiQuestionAnalysis analysis;
        if (regionFollowUpQuestion.isPresent()) {
            String resolvedRegionQuestion = regionFollowUpQuestion.get();
            analysis = forceResolvedTarget(
                    questionIntentClassifier.analyze(resolvedRegionQuestion),
                    resolvedRegionQuestion,
                    true);
        } else {
            analysis = conversationContext.hasContext() && !selfContainedResourceQuestion
                    ? analyzeWithConversationContext(content, conversationContext)
                    : questionIntentClassifier.analyze(content);
        }
        boolean explicitSiteListRequest = siteListAnswerValidator.requiresValidation(content);
        if (explicitSiteListRequest && regionFollowUpQuestion.isEmpty()) {
            analysis = forceResolvedTarget(analysis, content, analysis.followUp());
        }

        AiQuestionIntent intent = analysis.intent();
        String resolvedQuestion = analysis.resolvedQuestion() == null
                ? content : analysis.resolvedQuestion();
        boolean siteListRequest = explicitSiteListRequest
                || siteListAnswerValidator.requiresValidation(resolvedQuestion);
        boolean followUp = analysis.followUp() && !selfContainedResourceQuestion;
        AiSearchScope searchScope = resolveSearchScope(intent, analysis.searchScope());
        if (nationwideResourceQuestion) {
            searchScope = AiSearchScope.GENERAL;
        }
        AiQuestionRegionResolver.RegionResolution regionResolution =
                questionRegionResolver.resolve(resolvedQuestion, profile);
        AiResolvedContext resolvedContext = resolveStructuredContext(
                content, analysis.resolvedContext(),
                conversationContext.immediatePreviousResolvedContext(),
                regionResolution, followUp || regionFollowUpQuestion.isPresent(),
                analysis.requestedResultCount());
        if (followUp && resolvedContext != null) {
            resolvedQuestion = resolvedContext.toResolvedQuestion(resolvedQuestion);
            regionResolution = questionRegionResolver.resolve(resolvedQuestion, profile);
        }
        boolean finalSiteListRequest = siteListRequest
                || siteListAnswerValidator.requiresValidation(resolvedQuestion);
        if (intent == AiQuestionIntent.NONE
                && !regionResolution.isNotFound()
                && isLocalResourceTarget(resolvedQuestion)) {
            searchScope = AiSearchScope.LOCAL_RESOURCE;
        }
        if (finalSiteListRequest && regionResolution.isResolved()) {
            searchScope = AiSearchScope.LOCAL_RESOURCE;
        }
        InfoSubCategory category = finalSiteListRequest
                ? null : resolveInfoSubCategory(resolvedQuestion, analysis.infoSubCategory());
        AiQuestionRegionResolver.RegionResolution priorityRegion =
                resolveNationwideSearchPriorityRegion(
                        category, profile, conversationContext,
                        regionResolution, nationwideResourceQuestion);
        AiUserProfile searchProfile = (priorityRegion.isResolved()
                ? priorityRegion.applyTo(profile) : profile).withInfoSubCategory(category);

        return new AiQuestionContext(
                searchProfile, intent.starterQuestionType(), intent.safetyGuidance(),
                searchScope,
                intent == AiQuestionIntent.NONE ? analysis.retrievalQueries() : List.of(),
                analysis.requestedResultCount() == null && resolvedContext != null
                        ? resolvedContext.requestedResultCount()
                        : analysis.requestedResultCount(),
                resolvedQuestion, followUp, analysis.searchGoal(), analysis.requiredConcepts(),
                analysis.needsClarification(), analysis.clarificationQuestion(), resolvedContext);
    }

    public boolean isLocalResourceTarget(String question) {
        return question != null && (LOCAL_RESOURCE_PATTERN.matcher(question).find()
                || siteListAnswerValidator.requiresValidation(question));
    }

    public boolean isSelfContainedResourceQuestion(
            String originalQuestion,
            String resolvedQuestion,
            AiQuestionRegionResolver.RegionResolution regionResolution
    ) {
        String question = resolvedQuestion == null || resolvedQuestion.isBlank()
                ? originalQuestion : resolvedQuestion;
        if (!isLocalResourceTarget(question)
                || CONTEXT_REFERENCE_PATTERN.matcher(originalQuestion).find()) {
            return false;
        }
        return RELATIVE_LOCAL_REGION_PATTERN.matcher(originalQuestion).find()
                || regionResolution.isResolved()
                || isNationwideResourceQuestion(originalQuestion, regionResolution);
    }

    private AiQuestionAnalysis analyzeWithConversationContext(
            String content,
            AiConversationContext context
    ) {
        if (context.immediatePreviousResolvedContext() == null) {
            return questionIntentClassifier.analyze(
                    content, context.previousUserQuestion(), context.previousAiAnswer());
        }
        return questionIntentClassifier.analyze(
                content, context.previousUserQuestion(), context.previousAiAnswer(),
                context.immediatePreviousResolvedContext());
    }

    private Optional<String> resolveRegionOnlyFollowUpQuestion(
            String content,
            AiUserProfile profile,
            AiConversationContext context,
            AiQuestionRegionResolver.RegionResolution currentResolution
    ) {
        if (!context.hasContext() || context.immediatePreviousUserQuestion() == null
                || !questionRegionResolver.isRegionOnlyQuestion(content, currentResolution)) {
            return Optional.empty();
        }
        String previousQuestion = context.immediatePreviousUserQuestion();
        return Optional.of(questionRegionResolver.replaceRegion(
                previousQuestion,
                questionRegionResolver.resolve(previousQuestion, profile),
                currentResolution));
    }

    private AiQuestionAnalysis forceResolvedTarget(
            AiQuestionAnalysis analysis,
            String resolvedQuestion,
            boolean followUp
    ) {
        List<String> retrievalQueries = new ArrayList<>();
        retrievalQueries.add(resolvedQuestion);
        retrievalQueries.addAll(analysis.retrievalQueries());
        return new AiQuestionAnalysis(
                analysis.intent(), analysis.searchScope(), retrievalQueries,
                analysis.requestedResultCount(), resolvedQuestion, followUp,
                analysis.infoSubCategory(), analysis.searchGoal(), analysis.requiredConcepts(),
                false, null, analysis.resolvedContext());
    }

    private AiResolvedContext resolveStructuredContext(
            String content,
            AiResolvedContext analyzedContext,
            AiResolvedContext previousContext,
            AiQuestionRegionResolver.RegionResolution regionResolution,
            boolean followUp,
            Integer requestedResultCount
    ) {
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
        String normalized = normalize(content);
        if (followUp && normalized.contains("공립")) {
            resolved = resolved.withFilter("설립구분", "공립");
        } else if (followUp && normalized.contains("사립")) {
            resolved = resolved.withFilter("설립구분", "사립");
        }
        return resolved.isEmpty() ? null : resolved;
    }

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

    private AiSearchScope resolveSearchScope(
            AiQuestionIntent intent,
            AiSearchScope analyzedSearchScope
    ) {
        return switch (intent) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_RESOURCE;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION -> AiSearchScope.NATIONAL_POLICY;
            default -> analyzedSearchScope == null ? AiSearchScope.GENERAL : analyzedSearchScope;
        };
    }

    private InfoSubCategory resolveInfoSubCategory(
            String question,
            InfoSubCategory analyzedCategory
    ) {
        String normalized = normalize(question);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", "");
    }
}
