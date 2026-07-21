package com.bodeum.domain.info.entity;

import com.bodeum.domain.user.entity.User;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import com.bodeum.domain.info.entity.enums.ReportReasonType;
import com.bodeum.domain.info.entity.enums.ReportStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "info_review_report",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "info_review_id"})
        }
)
public class InfoReviewReport extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_review_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "info_review_id", nullable = false)
    private InfoReview infoReview;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_type", nullable = false)
    private ReportReasonType reasonType;

    @Column(length = 500)
    private String content;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Builder
    public InfoReviewReport(User user, InfoReview infoReview, ReportReasonType reasonType, String content) {
        this.user = user;

        this.infoReview = infoReview;
        this.reasonType = reasonType;
        this.content = content;
        this.status = ReportStatus.PENDING;
    }

    public void process(ReportStatus status) {
        this.status = status;
        this.processedAt = LocalDateTime.now();
    }
}
