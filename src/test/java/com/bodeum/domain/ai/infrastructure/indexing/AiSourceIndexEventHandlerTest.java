package com.bodeum.domain.ai.infrastructure.indexing;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.indexing.AiSourceChangedEvent;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.news.repository.NewsRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AiSourceIndexEventHandlerTest {

    private final InfoItemRepository infoItemRepository = mock(InfoItemRepository.class);
    private final NewsRepository newsRepository = mock(NewsRepository.class);
    private final AiContentIndexingService indexingService = mock(AiContentIndexingService.class);
    private final AiSourceIndexEventHandler handler = new AiSourceIndexEventHandler(
            infoItemRepository, newsRepository, indexingService);

    @Test
    void replacesInfoAfterCommittedUpsert() {
        InfoItem info = mock(InfoItem.class);
        when(infoItemRepository.findIndexableById(12L)).thenReturn(Optional.of(info));

        handler.handle(new AiSourceChangedEvent(
                AiResponseSourceType.INFO,
                12L,
                AiSourceChangedEvent.ChangeType.UPSERT
        ));

        verify(indexingService).replaceInfo(info);
    }

    @Test
    void deletesNewsDocumentWhenSourceIsNoLongerIndexable() {
        when(newsRepository.findIndexableById(35L)).thenReturn(Optional.empty());

        handler.handle(new AiSourceChangedEvent(
                AiResponseSourceType.NEWS,
                35L,
                AiSourceChangedEvent.ChangeType.UPSERT
        ));

        verify(indexingService).delete(AiResponseSourceType.NEWS, 35L);
    }

    @Test
    void deletesDocumentAfterSourceDeletion() {
        handler.handle(new AiSourceChangedEvent(
                AiResponseSourceType.INFO,
                12L,
                AiSourceChangedEvent.ChangeType.DELETE
        ));

        verify(indexingService).delete(AiResponseSourceType.INFO, 12L);
    }
}
