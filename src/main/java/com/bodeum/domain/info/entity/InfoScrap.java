package com.bodeum.domain.info.entity;

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
                @UniqueConstraint(columnNames = {"user_id", "info_id"})
        }
)
public class InfoScrap extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_scrap_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

//    # User 엔티티 구현 이후 FK 연결 예정
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_item_id", nullable = false)
    private InfoItem infoItem;

    @Builder
    public InfoScrap(Long userId, InfoItem infoItem) {

        // User 엔티티 연결 이후 수정 예정
        this.userId = userId;

        this.infoItem = infoItem;
    }
}
