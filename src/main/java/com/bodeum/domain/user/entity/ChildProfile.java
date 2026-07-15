package com.bodeum.domain.user.entity;

import com.bodeum.domain.user.enumtype.DisabilityType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "child_profiles")
public class ChildProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 20)
    private String nickname;

    @Column(length = 7)
    private String birth;

    @Column(name = "keyword_text", length = 100)
    private String keywordText;

    @OneToMany(mappedBy = "childProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChildDisability> disabilities = new ArrayList<>();

    protected ChildProfile() {
    }

    private ChildProfile(
            User user,
            String nickname,
            String birth,
            List<DisabilityType> disabilityTypes,
            String keywordText
    ) {
        this.user = user;
        update(nickname, birth, disabilityTypes, keywordText);
    }

    public static ChildProfile create(
            User user,
            String nickname,
            String birth,
            List<DisabilityType> disabilityTypes,
            String keywordText
    ) {
        return new ChildProfile(user, nickname, birth, disabilityTypes, keywordText);
    }

    public void update(
            String nickname,
            String birth,
            List<DisabilityType> disabilityTypes,
            String keywordText
    ) {
        this.nickname = nickname;
        this.birth = birth;
        this.keywordText = blankToNull(keywordText);
        replaceDisabilities(disabilityTypes);
    }

    public void updatePartial(
            String nickname,
            String birth,
            List<DisabilityType> disabilityTypes,
            String keywordText
    ) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }

        if (birth != null) {
            this.birth = birth;
        }

        if (keywordText != null) {
            this.keywordText = blankToNull(keywordText);
        }

        if (disabilityTypes != null) {
            replaceDisabilities(disabilityTypes);
        }
    }

    private void replaceDisabilities(List<DisabilityType> disabilityTypes) {
        disabilities.clear();
        disabilityTypes.forEach(disabilityType ->
                disabilities.add(ChildDisability.create(this, disabilityType))
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public boolean isRegistered() {
        return birth != null && !disabilities.isEmpty();
    }

    public String getNickname() {
        return nickname;
    }

    public String getBirth() {
        return birth;
    }

    public Integer getAge() {
        if (birth == null) {
            return null;
        }

        YearMonth birthYearMonth;
        try {
            birthYearMonth = YearMonth.parse(birth);
        } catch (DateTimeParseException e) {
            return null;
        }

        LocalDate now = LocalDate.now();
        int age = now.getYear() - birthYearMonth.getYear();
        if (now.getMonthValue() < birthYearMonth.getMonthValue()) {
            age--;
        }

        return age;
    }

    public List<DisabilityType> getDisabilityTypes() {
        return disabilities.stream()
                .map(ChildDisability::getDisabilityType)
                .toList();
    }

    public String getKeywordText() {
        return keywordText;
    }
}
