package com.bodeum.domain.info.entity;

import com.bodeum.domain.user.entity.User;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "info_scrap",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "info_item_id"})
        }
)
public class InfoScrap extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_scrap_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_item_id", nullable = false)
    private InfoItem infoItem;

    @Builder
    public InfoScrap(User user, InfoItem infoItem) {
        this.user = user;
        this.infoItem = infoItem;
    }
}
