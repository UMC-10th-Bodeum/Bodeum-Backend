package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiConversationContextService {

    private static final Pattern ADDITIONAL_RESULTS_PATTERN = Pattern.compile(
            "(?:(?:\\d+(?:개|곳)?)?더(?:\\d+(?:개|곳)?)?(?:많은)?"
                    + "(?:곳|기관|학교|센터|서비스|제도|항목)?"
                    + "|좀더(?:많은)?(?:곳|기관|학교|센터|서비스|제도|항목)?"
                    + "|나머지(?:것|기관|학교|센터|서비스|제도|항목)?"
                    + "|추가로(?:\\d+(?:개|곳)?)?"
                    + "|다른(?:곳|기관|학교|센터|서비스|제도|항목))"
                    + "(?:알려줘|알려주세요|추천해줘|추천해주세요)$");

    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiAnswerEvidenceService evidenceService;

    @Value("${bodeum.ai.conversation.recent-turn-count:3}")
    private int recentConversationTurnCount = 3;

    public AiConversationContext resolve(Long chatRoomId) {
        int turnCount = Math.max(1, recentConversationTurnCount);
        List<AiMessage> recentUsers = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.USER, PageRequest.of(0, turnCount + 1));
        if (recentUsers.size() < 2) {
            return AiConversationContext.empty();
        }
        List<AiMessage> previousAnswers = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.AI, PageRequest.of(0, turnCount));
        if (previousAnswers.isEmpty()) {
            previousAnswers = aiMessageRepository
                    .findTopByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                            chatRoomId, SenderType.AI)
                    .map(List::of).orElseGet(List::of);
        }
        if (previousAnswers.isEmpty()) {
            return AiConversationContext.empty();
        }
        List<AiMessage> previousUsers = recentUsers.subList(
                1, Math.min(recentUsers.size(), turnCount + 1));
        AiMessage immediatePrevious = previousUsers.getFirst();
        Long rootId = immediatePrevious.getContextRootMessageId();
        return new AiConversationContext(
                formatQuestions(previousUsers), formatAnswers(previousAnswers),
                resolvedQuestion(immediatePrevious), immediatePrevious.getResolvedContext(),
                immediatePrevious.getId(),
                rootId == null || rootId <= 0 ? immediatePrevious.getId() : rootId);
    }

    public AiAdditionalResultsContext resolveAdditionalResults(Long chatRoomId, String content) {
        if (!isAdditionalResultsQuestion(content)) {
            return AiAdditionalResultsContext.empty();
        }
        List<AiMessage> nearestUsers = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.USER, PageRequest.of(0, 2));
        if (nearestUsers.size() < 2) {
            return AiAdditionalResultsContext.empty();
        }
        Long rootId = nearestUsers.get(1).getContextRootMessageId();
        if (rootId == null || rootId <= 0) {
            return AiAdditionalResultsContext.empty();
        }
        Optional<AiMessage> root = aiMessageRepository.findById(rootId);
        List<AiMessage> previousAnswers = aiMessageRepository
                .findByChatRoomIdAndContextRootMessageIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, rootId, SenderType.AI);
        if (root.isEmpty() || previousAnswers.isEmpty()) {
            return AiAdditionalResultsContext.empty();
        }
        List<AiResponseSourceProjection> sources = aiResponseSourceRepository
                .findAllByMessageIds(previousAnswers.stream().map(AiMessage::getId).toList());
        if (sources.isEmpty()) {
            return AiAdditionalResultsContext.empty();
        }
        Set<AiSourceKey> excludedSources = sources.stream()
                .map(source -> new AiSourceKey(source.getSourceType(), source.getSourceId()))
                .collect(java.util.stream.Collectors.toSet());
        List<String> excludedTitles = sources.stream()
                .map(AiResponseSourceProjection::getSourceTitle)
                .filter(title -> title != null && !title.isBlank()).distinct().toList();
        Set<String> excludedIdentityKeys = sources.stream()
                .flatMap(source -> evidenceService.sourceIdentityKeys(
                        source.getSourceTitle(), source.getSourceUrl()).stream())
                .collect(java.util.stream.Collectors.toSet());
        return new AiAdditionalResultsContext(
                resolvedQuestion(root.get()), excludedSources, excludedTitles,
                excludedIdentityKeys);
    }

    private boolean isAdditionalResultsQuestion(String content) {
        String normalized = normalize(content);
        if (normalized.contains("자세히") || normalized.contains("상세히")
                || normalized.contains("내용") || normalized.contains("방법")) {
            return false;
        }
        return ADDITIONAL_RESULTS_PATTERN.matcher(normalized).find();
    }

    private String resolvedQuestion(AiMessage message) {
        return message.getResolvedQuestion() == null || message.getResolvedQuestion().isBlank()
                ? message.getContent() : message.getResolvedQuestion();
    }

    private String formatQuestions(List<AiMessage> messages) {
        return format(messages, true);
    }

    private String formatAnswers(List<AiMessage> messages) {
        return format(messages, false);
    }

    private String format(List<AiMessage> messages, boolean question) {
        if (messages.size() == 1) {
            return question ? resolvedQuestion(messages.getFirst()) : messages.getFirst().getContent();
        }
        StringBuilder context = new StringBuilder();
        for (int index = messages.size() - 1; index >= 0; index--) {
            String value = question ? resolvedQuestion(messages.get(index))
                    : messages.get(index).getContent();
            if (value != null && !value.isBlank()) {
                context.append("- ").append(value).append('\n');
            }
        }
        return context.toString().trim();
    }

    private String normalize(String content) {
        return content == null ? "" : content.trim().replaceAll("\\s+", "");
    }
}
