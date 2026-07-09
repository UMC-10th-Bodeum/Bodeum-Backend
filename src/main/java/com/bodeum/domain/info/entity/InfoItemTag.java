package com.bodeum.domain.info.entity;

import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "info_item_tag",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"info_id", "info_tag_id"})
        }
)
public class InfoItemTag extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_item_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_id", nullable = false)
    private InfoItem infoItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_tag_id", nullable = false)
    private InfoTag infoTag;

    @Builder
    public InfoItemTag(InfoItem infoItem, InfoTag infoTag) {
        this.infoItem = infoItem;
        this.infoTag = infoTag;
    }
}
