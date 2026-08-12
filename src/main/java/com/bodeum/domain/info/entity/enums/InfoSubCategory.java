package com.bodeum.domain.info.entity.enums;

public enum InfoSubCategory {
    HOSPITAL_ETC,
    GENERAL_HOSPITAL,
    PRIMARY_CARE,
    EMERGENCY_CLINIC,
    INSTITUTION_ETC,
    THERAPY_REHAB,
    WELFARE_CENTER,
    YOUTH_CENTER,
    FAMILY_SUPPORT,
    WELFARE_ETC,
    PRIVATE_WELFARE,
    NATIONAL_WELFARE,
    LOCAL_WELFARE,
    EDUCATION_ETC,
    SPECIAL_SCHOOL,
    SPECIAL_EDU_SUPPORT,
    LIFELONG_EDU,
    EMPLOYMENT_ETC,
    REALTIME_JOB,
    KEAD_JOB,
    STANDARD_WORKPLACE;

    public boolean isRecommendationCategory() {
        return name().endsWith("_ETC");
    }
}
