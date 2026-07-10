package com.bodeum.domain.info.entity;

import com.bodeum.domain.info.entity.enums.DayOfWeek;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "info_operating_hour")
public class InfoOperatingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_operating_hour_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_item_id", nullable = false)
    private InfoItem infoItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "break_start_time")
    private LocalTime breakStartTime;

    @Column(name = "break_end_time")
    private LocalTime breakEndTime;

    @Column(name = "is_closed", nullable = false)
    private boolean isClosed = false;

    @Column(length = 30)
    private String note;

    @Builder
    public InfoOperatingHour(InfoItem infoItem, DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime,
                             LocalTime breakStartTime, LocalTime breakEndTime, boolean isClosed, String note) {
        this.infoItem = infoItem;
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.breakStartTime = breakStartTime;
        this.breakEndTime = breakEndTime;
        this.isClosed = isClosed;
        this.note = note;
    }
}
