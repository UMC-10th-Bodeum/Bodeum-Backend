package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;

import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.context.AiConversationContext;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.ai.repository.projection.AiConversationMessageProjection;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최근 대화 이력을 기반으로 AI 질문에 필요한 대화 문맥을 구성하고,
 * 추가 결과 요청 시 이전 답변의 출처를 제외하기 위한 문맥을 생성한다.
 */
@Service
@RequiredArgsConstructor
public class AiConversationContextService {

    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiAnswerEvidenceService evidenceService;

    @Value("${bodeum.ai.conversation.recent-turn-count:5}")
    private int recentConversationTurnCount = 5;

    @Transactional(readOnly = true)
    public AiConversationContext resolve(Long chatRoomId) {
        int turnCount = Math.max(1, recentConversationTurnCount);

        // 최근 사용자·AI 메시지를 한 번에 조회해 대화 문맥 구성
        List<AiConversationMessageProjection> recentConversation = aiMessageRepository
                .findRecentConversationContext(
                        chatRoomId, PageRequest.of(0, turnCount * 2 + 1));
        if (!recentConversation.isEmpty()) {
            return resolveProjectedContext(recentConversation, turnCount);
        }

        // Projection 조회 결과가 없으면 USER/AI 메시지 개별 조회 방식으로 폴백
        List<AiMessage> recentUsers = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.USER, PageRequest.of(0, turnCount + 1));
        if (recentUsers.size() < 2) {
            return AiConversationContext.empty();
        }
        List<AiMessage> previousAnswers = aiMessageRepository
                .findByChatRoomIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, SenderType.AI, PageRequest.of(0, 1));
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
                formatLegacyConversation(previousUsers, previousAnswers),
                formatQuestions(previousUsers), formatAnswers(previousAnswers),
                resolvedQuestion(immediatePrevious), immediatePrevious.getResolvedContext(),
                immediatePrevious.getId(),
                rootId == null || rootId <= 0 ? immediatePrevious.getId() : rootId);
    }

    private AiConversationContext resolveProjectedContext(
            List<AiConversationMessageProjection> recentConversation,
            int turnCount
    ) {
        // 현재 질문을 포함한 최근 사용자 메시지를 추출
        List<AiConversationMessageProjection> recentUsers = recentConversation.stream()
                .filter(message -> message.getSenderType() == SenderType.USER)
                .limit(turnCount + 1L)
                .toList();
        if (recentUsers.size() < 2) {
            return AiConversationContext.empty();
        }
        Optional<AiConversationMessageProjection> previousAnswer = recentConversation.stream()
                .filter(message -> message.getSenderType() == SenderType.AI)
                .findFirst();
        if (previousAnswer.isEmpty()) {
            return AiConversationContext.empty();
        }

        // 첫 번째 사용자 메시지는 현재 질문이므로 제외하고 이전 질문만 사용
        List<AiConversationMessageProjection> previousUsers = recentUsers.subList(
                1, Math.min(recentUsers.size(), turnCount + 1));
        AiConversationMessageProjection immediatePrevious = previousUsers.getFirst();
        Long rootId = immediatePrevious.getContextRootMessageId();
        return new AiConversationContext(
                formatRecentConversation(recentConversation, turnCount),
                formatProjectedQuestions(previousUsers),
                resolvedAnswer(previousAnswer.get()),
                resolvedQuestion(immediatePrevious),
                immediatePrevious.getResolvedContext(),
                immediatePrevious.getId(),
                rootId == null || rootId <= 0 ? immediatePrevious.getId() : rootId);
    }

    private String formatRecentConversation(
            List<AiConversationMessageProjection> messages,
            int turnCount
    ) {
        List<AiConversationMessageProjection> selected = new java.util.ArrayList<>();
        List<AiConversationMessageProjection> pendingAnswers = new java.util.ArrayList<>();
        boolean currentUserSkipped = false;
        int selectedUserCount = 0;

        // 현재 사용자 질문은 제외하고, 이전 사용자 질문을 기준으로 최근 turnCount개 대화를 선택
        for (AiConversationMessageProjection message : messages) {
            if (!currentUserSkipped && message.getSenderType() == SenderType.USER) {
                currentUserSkipped = true;
                continue;
            }
            if (!currentUserSkipped) {
                continue;
            }
            if (message.getSenderType() == SenderType.USER) {
                if (selectedUserCount >= turnCount) {
                    break;
                }
                selected.addAll(pendingAnswers);
                pendingAnswers.clear();
                selected.add(message);
                selectedUserCount++;
                continue;
            }
            if (message.getSenderType() == SenderType.AI) {
                pendingAnswers.add(message);
            }
        }
        StringBuilder conversation = new StringBuilder();

        // 조회 결과는 최신순이므로 오래된 대화부터 사용자/AI 순서로 구성
        for (int index = selected.size() - 1; index >= 0; index--) {
            AiConversationMessageProjection message = selected.get(index);
            String content = message.getSenderType() == SenderType.USER
                    ? resolvedQuestion(message) : resolvedAnswer(message);
            if (content != null && !content.isBlank()) {
                conversation.append(message.getSenderType() == SenderType.USER
                                ? "사용자: " : "AI: ")
                        .append(content.trim()).append('\n');
            }
        }
        return conversation.toString().trim();
    }

    private String formatLegacyConversation(
            List<AiMessage> users,
            List<AiMessage> answers
    ) {
        StringBuilder conversation = new StringBuilder();
        for (int index = users.size() - 1; index >= 0; index--) {
            conversation.append("사용자: ").append(resolvedQuestion(users.get(index))).append('\n');
            if (index < answers.size()) {
                String answer = answers.get(index).getContent();
                if (answer != null && !answer.isBlank()) {
                    conversation.append("AI: ").append(answer.trim()).append('\n');
                }
            }
        }
        return conversation.toString().trim();
    }

    @Transactional(readOnly = true)
    public AiAdditionalResultsContext resolveAdditionalResults(
            Long chatRoomId,
            AiConversationContext conversationContext,
            boolean excludePreviousResults
    ) {
        // 추가 결과 요청이 아니거나 이전 대화 문맥이 없으면 제외 출처를 구성하지 않음
        if (!excludePreviousResults
                || conversationContext == null
                || conversationContext.rootUserMessageId() == null
                || conversationContext.immediatePreviousUserQuestion() == null) {
            return AiAdditionalResultsContext.empty();
        }
        Long rootId = conversationContext.rootUserMessageId();
        if (rootId <= 0) {
            return AiAdditionalResultsContext.empty();
        }
        List<AiMessage> previousAnswers = aiMessageRepository
                .findByChatRoomIdAndContextRootMessageIdAndSenderTypeOrderByCreatedAtDescIdDesc(
                        chatRoomId, rootId, SenderType.AI);
        Optional<AiMessage> root = aiMessageRepository.findById(rootId);
        if (root.isEmpty() || previousAnswers.isEmpty()) {
            return AiAdditionalResultsContext.empty();
        }

        // 동일한 대화 흐름에서 이전 AI 답변에 사용된 출처 조회
        List<AiResponseSourceProjection> sources = aiResponseSourceRepository
                .findAllByMessageIds(previousAnswers.stream().map(AiMessage::getId).toList());
        if (sources.isEmpty()) {
            return AiAdditionalResultsContext.empty();
        }

        // 추가 검색에서 이전 출처가 다시 노출되지 않도록 제외 정보 구성
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
                resolvedQuestion(root.get()),
                excludedSources, excludedTitles,
                excludedIdentityKeys);
    }

    private String resolvedQuestion(AiMessage message) {
        return message.getResolvedQuestion() == null || message.getResolvedQuestion().isBlank()
                ? message.getContent() : message.getResolvedQuestion();
    }

    private String resolvedQuestion(AiConversationMessageProjection message) {
        return message.getResolvedQuestion() == null || message.getResolvedQuestion().isBlank()
                ? message.getContent() : message.getResolvedQuestion();
    }

    private String resolvedAnswer(AiConversationMessageProjection message) {
        return message.getContent() == null ? "" : message.getContent();
    }

    private String formatProjectedQuestions(
            List<AiConversationMessageProjection> messages
    ) {
        if (messages.size() == 1) {
            return resolvedQuestion(messages.getFirst());
        }
        StringBuilder context = new StringBuilder();
        for (int index = messages.size() - 1; index >= 0; index--) {
            String value = resolvedQuestion(messages.get(index));
            if (value != null && !value.isBlank()) {
                context.append("- ").append(value).append('\n');
            }
        }
        return context.toString().trim();
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

}
