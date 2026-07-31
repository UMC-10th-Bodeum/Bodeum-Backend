package com.bodeum.domain.news.infrastructure.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.news.entity.NewsCategoryCode;
import org.junit.jupiter.api.Test;

class NewsCategoryClassifierTest {

    @Test
    void mapsVoucherAndSubsidyPrograms() {
        assertCategory(
                data("바우처", "바우처", "발달재활 바우처 치료", null),
                NewsCategoryCode.VOUCHER_SUBSIDY
        );
    }

    @Test
    void mapsRecruitmentAndParticipationPrograms() {
        assertCategory(
                data(null, null, "부모 힐링캠프 참가자 모집", null),
                NewsCategoryCode.RECRUITMENT_PARTICIPATION
        );
    }

    @Test
    void mapsEducationAndSeminarPrograms() {
        assertCategory(
                data("교육재활", null, "장애인식개선교육", null),
                NewsCategoryCode.EDUCATION_SEMINAR
        );
    }

    @Test
    void mapsWelfareServicePrograms() {
        assertCategory(
                data("의료재활", "언어치료", "언어치료", "개별 언어치료 진행"),
                NewsCategoryCode.BENEFIT_WELFARE_SERVICE
        );
    }

    @Test
    void mapsInstitutionNoticesAndNews() {
        assertCategory(
                data(null, null, "복지관 임시 휴관 공지", null),
                NewsCategoryCode.INSTITUTION_NOTICE_NEWS
        );
    }

    @Test
    void doesNotTreatNewSportsAsInstitutionNews() {
        assertCategory(
                data(null, null, "뉴스포츠 교실", null),
                NewsCategoryCode.EDUCATION_SEMINAR
        );
    }

    @Test
    void doesNotCreateNoticeKeywordAcrossWordBoundaries() {
        assertCategory(
                data(null, null, "장애인 즐거운 한마당", "기념행사 통한 긍정적 인식제공 지원"),
                NewsCategoryCode.RECRUITMENT_PARTICIPATION
        );
    }

    @Test
    void distinguishesEquipmentChecksFromInstitutionNotices() {
        assertCategory(
                data("기타", "복지용구공유플랫폼", "급속충전기 점검수리사업", null),
                NewsCategoryCode.BENEFIT_WELFARE_SERVICE
        );
        assertCategory(
                data("기타", "보조기기서비스", "이동기기 자가점검 교육사업", null),
                NewsCategoryCode.EDUCATION_SEMINAR
        );
    }

    @Test
    void defaultsUnclassifiedWelfareProgramsToWelfareService() {
        assertCategory(
                data(null, null, "사랑의 도시락 나눔", "중식 제공"),
                NewsCategoryCode.BENEFIT_WELFARE_SERVICE
        );
    }

    private void assertCategory(
            DisabledWelfareProgramData data,
            NewsCategoryCode expected
    ) {
        assertThat(NewsCategoryClassifier.classifyActivity(data)).isEqualTo(expected);
    }

    private DisabledWelfareProgramData data(
            String category,
            String detailCategory,
            String programName,
            String programContent
    ) {
        return new DisabledWelfareProgramData(
                "수원시",
                "테스트 복지관",
                null,
                null,
                null,
                null,
                category,
                detailCategory,
                programName,
                programContent,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "2026-07-29"
        );
    }
}
