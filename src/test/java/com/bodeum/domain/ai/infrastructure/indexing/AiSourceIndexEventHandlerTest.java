package com.bodeum.domain.ai.infrastructure.indexing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.indexing.AiSourceChangedEvent;
import org.junit.jupiter.api.Test;

class AiSourceIndexEventHandlerTest {

    private final AiContentIndexingService indexingService = mock(AiContentIndexingService.class);
    private final AiSourceIndexEventHandler handler =
            new AiSourceIndexEventHandler(indexingService);

    @Test
    void synchronizesInfoAfterCommittedUpsert() {
        handler.handle(new AiSourceChangedEvent(
                AiResponseSourceType.INFO,
                12L,
                AiSourceChangedEvent.ChangeType.UPSERT
        ));

        verify(indexingService).synchronize(AiResponseSourceType.INFO, 12L);
    }

    @Test
    void synchronizesNewsAfterCommittedUpsert() {
        handler.handle(new AiSourceChangedEvent(
                AiResponseSourceType.NEWS,
                35L,
                AiSourceChangedEvent.ChangeType.UPSERT
        ));

        verify(indexingService).synchronize(AiResponseSourceType.NEWS, 35L);
    }

    @Test
    void synchronizesLatestStateAfterSourceDeletion() {
        handler.handle(new AiSourceChangedEvent(
                AiResponseSourceType.INFO,
                12L,
                AiSourceChangedEvent.ChangeType.DELETE
        ));

        verify(indexingService).synchronize(AiResponseSourceType.INFO, 12L);
    }
}
