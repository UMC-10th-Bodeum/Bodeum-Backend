package com.bodeum.domain.ai.dto.response;

import java.time.LocalDate;
import java.util.List;

public record AiMessageHistoryResponse(
        List<HistoryDateGroup> messages,
        Long nextCursor,
        boolean hasNext
) {

    public AiMessageHistoryResponse {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public static AiMessageHistoryResponse of(
            List<HistoryDateGroup> messages,
            Long nextCursor,
            boolean hasNext
    ) {
        return new AiMessageHistoryResponse(messages, nextCursor, hasNext);
    }

    public record HistoryDateGroup(
            LocalDate date,
            List<AiMessageResponse> items
    ) {

        public HistoryDateGroup {
            items = items == null ? List.of() : List.copyOf(items);
        }

        public static HistoryDateGroup of(
                LocalDate date,
                List<AiMessageResponse> items
        ) {
            return new HistoryDateGroup(date, items);
        }
    }
}
