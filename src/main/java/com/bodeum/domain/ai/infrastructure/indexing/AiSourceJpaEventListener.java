package com.bodeum.domain.ai.infrastructure.indexing;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.indexing.AiSourceChangedEvent;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.news.entity.News;
import java.util.Arrays;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class AiSourceJpaEventListener implements
        PostInsertEventListener,
        PostUpdateEventListener,
        PostDeleteEventListener {

    private static final Set<String> INFO_INDEXED_PROPERTIES = Set.of(
            "infoCategory", "name", "introduction", "address", "sido", "sigungu",
            "phone", "homepageUrl"
    );
    private static final Set<String> NEWS_IGNORED_PROPERTIES = Set.of(
            "viewCount", "scrapCount", "updatedAt"
    );

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void onPostInsert(PostInsertEvent event) {
        publish(event.getEntity(), AiSourceChangedEvent.ChangeType.UPSERT);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        Object entity = event.getEntity();
        if (entity instanceof InfoItem && !hasIndexedInfoChange(event)) {
            return;
        }
        if (entity instanceof News && !hasIndexedNewsChange(event)) {
            return;
        }
        publish(entity, AiSourceChangedEvent.ChangeType.UPSERT);
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        publish(event.getEntity(), AiSourceChangedEvent.ChangeType.DELETE);
    }

    private boolean hasIndexedInfoChange(PostUpdateEvent event) {
        return dirtyPropertyNames(event).stream().anyMatch(INFO_INDEXED_PROPERTIES::contains);
    }

    private boolean hasIndexedNewsChange(PostUpdateEvent event) {
        return dirtyPropertyNames(event).stream().anyMatch(
                property -> !NEWS_IGNORED_PROPERTIES.contains(property));
    }

    private Set<String> dirtyPropertyNames(PostUpdateEvent event) {
        int[] dirtyProperties = event.getDirtyProperties();
        if (dirtyProperties == null) {
            return Set.of(event.getPersister().getPropertyNames());
        }
        String[] propertyNames = event.getPersister().getPropertyNames();
        return Arrays.stream(dirtyProperties)
                .filter(index -> index >= 0 && index < propertyNames.length)
                .mapToObj(index -> propertyNames[index])
                .collect(java.util.stream.Collectors.toSet());
    }

    private void publish(Object entity, AiSourceChangedEvent.ChangeType changeType) {
        if (entity instanceof InfoItem info && info.getId() != null) {
            eventPublisher.publishEvent(new AiSourceChangedEvent(
                    AiResponseSourceType.INFO, info.getId(), changeType));
        } else if (entity instanceof News news && news.getId() != null) {
            eventPublisher.publishEvent(new AiSourceChangedEvent(
                    AiResponseSourceType.NEWS, news.getId(), changeType));
        }
    }
}
