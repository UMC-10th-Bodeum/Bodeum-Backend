package com.bodeum.domain.news.entity;

import com.bodeum.global.common.entity.BaseCreatedUpdatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(name = "news_source")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NewsSource extends BaseCreatedUpdatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_source_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private NewsSourceType sourceType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    @Column(name = "list_url", length = 500)
    private String listUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    public void updateLastSyncedAt(Instant lastSyncedAt) {
        this.lastSyncedAt = lastSyncedAt;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isCollectable() {
        return Boolean.TRUE.equals(this.active);
    }
}
