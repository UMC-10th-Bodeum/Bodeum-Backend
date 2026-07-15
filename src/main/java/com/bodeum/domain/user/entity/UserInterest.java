package com.bodeum.domain.user.entity;

import com.bodeum.domain.user.enumtype.InterestCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_interests")
public class UserInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_category", nullable = false, length = 50)
    private InterestCategory interestCategory;

    protected UserInterest() {
    }

    private UserInterest(User user, InterestCategory interestCategory) {
        this.user = user;
        this.interestCategory = interestCategory;
    }

    public static UserInterest create(User user, InterestCategory interestCategory) {
        return new UserInterest(user, interestCategory);
    }

    public InterestCategory getInterestCategory() {
        return interestCategory;
    }
}
