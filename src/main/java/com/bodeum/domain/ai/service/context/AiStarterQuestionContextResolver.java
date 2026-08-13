package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiQuestionContext;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public Optional<AiQuestionContext> resolve(
            Long chatRoomId,
            String content,
            AiUserProfile profile
    ) {
        Optional<Region> explicitRegion = resolveExplicitRehabRegion(content);
        if (explicitRegion.isPresent()) {
            return Optional.of(localRehabContext(profile, explicitRegion.get()));
        }
        Optional<AiStarterQuestionType> questionType =
                AiStarterQuestionType.fromQuestion(content);
        if (questionType.isPresent()) {
            return Optional.of(starterQuestionContext(profile, questionType.get()));
        }
        return resolveRegionFollowUp(chatRoomId, content)
                .map(region -> localRehabContext(profile, region));
    }

    public boolean shouldRoute(AiQuestionContext context) {
        return context.questionType().orElse(null) != AiStarterQuestionType.WELFARE_SITES
                || context.searchScope() != AiSearchScope.LOCAL_RESOURCE;
    }

    private AiQuestionContext starterQuestionContext(
            AiUserProfile profile,
            AiStarterQuestionType questionType
    ) {
        return new AiQuestionContext(
                profile, Optional.of(questionType), Optional.empty(), searchScope(questionType),
                List.of(), null, null, false, null, List.of(), false, null, null,
                questionType == AiStarterQuestionType.WELFARE_SITES);
    }

    private AiSearchScope searchScope(AiStarterQuestionType questionType) {
        return switch (questionType) {
            case LOCAL_REHAB_CENTERS -> AiSearchScope.LOCAL_RESOURCE;
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION -> AiSearchScope.NATIONAL_POLICY;
            default -> AiSearchScope.GENERAL;
        };
    }

    private AiQuestionContext localRehabContext(AiUserProfile profile, Region region) {
        return starterQuestionContext(
                profile.withRegion(
                        region.getFullName(), region.getRegionLevel1(), region.getRegionLevel2()),
                AiStarterQuestionType.LOCAL_REHAB_CENTERS);
    }

    private Optional<Region> resolveExplicitRehabRegion(String content) {
        String normalizedQuestion = normalizeQuestion(content);
        boolean rehabRecommendation = normalizedQuestion.contains("재활센터")
                && (normalizedQuestion.contains("추천")
                || normalizedQuestion.contains("알려"));
        if (!rehabRecommendation) {
            return Optional.empty();
        }
        String normalizedContent = normalizeSpacing(content);
        return regionRepository.findMentionedInQuestion(
                        normalizedContent, PageRequest.of(0, 1)).stream()
                .filter(region -> isGenericRehabQuestion(normalizedContent, region))
                .findFirst();
    }

    private boolean isGenericRehabQuestion(String question, Region region) {
        String questionWithoutRegion = question.replace(region.getFullName(), " ")
                .trim().replaceFirst("^(에서|의|에|내)\\s*", "");
        String normalizedQuestion = normalizeQuestion(questionWithoutRegion)
                .replaceFirst("추천해주세요$", "추천해줘")
                .replaceFirst("알려주세요$", "알려줘");
        return EXPLICIT_REGION_REHAB_QUESTIONS.contains(normalizedQuestion);
    }

    private Optional<Region> resolveRegionFollowUp(Long chatRoomId, String content) {
        boolean awaitingRegion = aiMessageRepository
                .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.AI)
                .filter(message -> message.getContent() == null
                        || !message.getContent().startsWith(AMBIGUOUS_REGION_MESSAGE_PREFIX))
                .map(AiMessage::getAiAnswerStatus)
                .filter(status -> status == AiAnswerStatus.REGION_REQUIRED)
                .isPresent();
        if (!awaitingRegion) {
            return Optional.empty();
        }
        String regionName = normalizeSpacing(content)
                .replaceFirst("(입니다|이에요|예요|이야|야)$", "").trim();
        return regionName.isEmpty() ? Optional.empty()
                : regionRepository.findByFullName(regionName);
    }

    private String normalizeQuestion(String content) {
        return normalizeSpacing(content).replaceAll("\\s+", "");
    }

    private String normalizeSpacing(String content) {
        return content == null ? "" : content.trim()
                .replaceFirst("[.!?~]+$", "").trim().replaceAll("\\s+", " ");
    }
}
