package com.bodeum.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_agreements")
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "service_terms_agreed", nullable = false)
    private boolean serviceTermsAgreed;

    @Column(name = "privacy_policy_agreed", nullable = false)
    private boolean privacyPolicyAgreed;

    @Column(name = "ai_terms_agreed", nullable = false)
    private boolean aiTermsAgreed;

    @Column(name = "ai_terms_agreed_at")
    private Instant aiTermsAgreedAt;

    @Column(name = "agreed_at")
    private Instant agreedAt;

    protected UserAgreement() {
    }

    private UserAgreement(
            User user,
            boolean serviceTermsAgreed,
            boolean privacyPolicyAgreed,
            boolean aiTermsAgreed
    ) {
        this.user = user;
        agree(serviceTermsAgreed, privacyPolicyAgreed, aiTermsAgreed);
    }

    public static UserAgreement create(
            User user,
            boolean serviceTermsAgreed,
            boolean privacyPolicyAgreed,
            boolean aiTermsAgreed
    ) {
        return new UserAgreement(user, serviceTermsAgreed, privacyPolicyAgreed, aiTermsAgreed);
    }

    public void agree(
            boolean serviceTermsAgreed,
            boolean privacyPolicyAgreed,
            boolean aiTermsAgreed
    ) {
        Instant now = Instant.now();
        this.serviceTermsAgreed = serviceTermsAgreed;
        this.privacyPolicyAgreed = privacyPolicyAgreed;
        if (aiTermsAgreed && !this.aiTermsAgreed) {
            this.aiTermsAgreedAt = now;
        } else if (!aiTermsAgreed) {
            this.aiTermsAgreedAt = null;
        }
        this.aiTermsAgreed = aiTermsAgreed;
        this.agreedAt = now;
    }

    public boolean isRequiredAgreed() {
        return serviceTermsAgreed && privacyPolicyAgreed;
    }

    public boolean isServiceTermsAgreed() {
        return serviceTermsAgreed;
    }

    public boolean isPrivacyPolicyAgreed() {
        return privacyPolicyAgreed;
    }

    public boolean isAiTermsAgreed() {
        return aiTermsAgreed;
    }

    public Instant getAgreedAt() {
        return agreedAt;
    }

    public Instant getAiTermsAgreedAt() {
        return aiTermsAgreedAt;
    }
}
