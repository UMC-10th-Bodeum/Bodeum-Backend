package com.bodeum.global.infrastructure.constant;

import lombok.Getter;
import java.util.List;
import java.util.stream.Stream;

//오픈 API 수집 출처 목록(총 33개 항목)을 지정된 소분류 카테고리에 매핑.

@Getter
public enum OpenApiSourceSpec {

    // =========================================================================
    // 1. HOSPITAL 계열 (총 10개)
    // =========================================================================
    // [소분류: PRIMARY_CARE (id = 3)]
    ODCLOUD_DISABLED_PRIMARY_CARE(OpenApiCategory.PRIMARY_CARE, "ODCLOUD", "https://api.odcloud.kr/api/15144843/v1/uddi:76b1c743-cad2-4363-93cb-f13d79b00d0a", null),

    // [소분류: EMERGENCY_CLINIC (id = 4)]
    DATAGO_EMERGENCY_BED_REALTIME(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEmrrmRltmUsefulSckbdInfoInqire", null),
    DATAGO_SEVERE_DISEASE_ACCEPTANCE(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getSrsillDissAceptncPosblInfoInqire", null),
    DATAGO_EMERGENCY_MEDICAL_LIST(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEgytListInfoInqire", null),
    DATAGO_EMERGENCY_MEDICAL_BASIC(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEgytBassInfoInqire", null),
    DATAGO_TRAUMA_CENTER_LIST(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getStrmListInfoInqire", null),
    DATAGO_TRAUMA_CENTER_BASIC(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getStrmBassInfoInqire", null),
    DATAGO_EMERGENCY_CRITICAL_MSG(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEmrrmSrsillDissMsgInqire", null),
    DATAGO_NMC_NATIONAL_EMERGENCY(OpenApiCategory.EMERGENCY_CLINIC, "DATAGO", "http://apis.data.go.kr/B552657/ErmctInfoInqireService/getEmrrmRltmUsefulSckbdInfoInqire", null),

    // [소분류: HOSPITAL_ETC (id = 1)]
    GG_DISABLED_MEDICAL_REHAB(OpenApiCategory.HOSPITAL_ETC, "GG", "https://openapi.gg.go.kr/DisablePersonRehabilitation", "DisablePersonRehabilitation"),

    // =========================================================================
    // 2. INSTITUTION 계열 (총 4개)
    // =========================================================================
    // [소분류: THERAPY_REHAB (id = 6)] - 보건복지부 발달재활 제공기관 현황 매핑
    ODCLOUD_DEVELOPMENT_REHAB_PROVIDER(OpenApiCategory.THERAPY_REHAB, "ODCLOUD", "https://api.odcloud.kr/api/15066351/v1/uddi:cf3fa47b-fba7-4c54-8b3e-e0a94d4da3b6", null),

    // [소분류: WELFARE_CENTER (id = 7)]
    ODCLOUD_DISABLED_WELFARE_INST(OpenApiCategory.WELFARE_CENTER, "ODCLOUD", "https://api.odcloud.kr/api/15075529/v1/uddi:f153fd90-c36c-44d5-a9f1-8a0041cfa9b7", null),
    GG_DISABLED_ACTIVITY_SUPPORT_ORG(OpenApiCategory.WELFARE_CENTER, "GG", "https://openapi.gg.go.kr/Ggsigundspsnactsport", "Ggsigundspsnactsport"),

    // [소분류: FAMILY_SUPPORT (id = 9)]
    ODCLOUD_DEVELOPMENT_DISABLED_ORG_INFO(OpenApiCategory.FAMILY_SUPPORT, "ODCLOUD", "https://api.odcloud.kr/api/15127872/v1/uddi:c9f30dc4-d0bf-45c4-b93b-e062c648d0d4", null),

    // =========================================================================
    // 3. WELFARE 계열 (총 7개)
    // =========================================================================
    // [소분류: PRIVATE_WELFARE (id = 11)]
    ODCLOUD_PRIVATE_WELFARE_SERVICE(OpenApiCategory.PRIVATE_WELFARE, "ODCLOUD", "https://api.odcloud.kr/api/15116392/v1/uddi:44e91fb3-7ca8-4f83-a978-d42109ed8443", null),
    ODCLOUD_KODDI_DEVELOPMENT_DISABLED(OpenApiCategory.PRIVATE_WELFARE, "ODCLOUD", "https://api.odcloud.kr/api/15072489/v1/uddi:dca48d86-d758-4263-87d8-c57c35771591", null),

    // [소분류: NATIONAL_WELFARE (id = 12)]
    DATAGO_CENTRAL_WELFARE_INFO(OpenApiCategory.NATIONAL_WELFARE, "DATAGO", "https://apis.data.go.kr/B554287/NationalWelfareInformationsV001/NationalWelfarelistV001", null),

    // [소분류: LOCAL_WELFARE (id = 13)]
    DATAGO_LOCAL_GOVERNMENT_WELFARE(OpenApiCategory.LOCAL_WELFARE, "DATAGO", "https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations/LcgvWelfarelist", null),
    DATAGO_GG_WELFARE_CENTER_STATUS(OpenApiCategory.LOCAL_WELFARE, "DATAGO", "https://apis.data.go.kr/B554287/NationalWelfareInformationsV001/NationalWelfarelistV001", null),
    GG_DISABLED_WELFARE_FACILITY(OpenApiCategory.LOCAL_WELFARE, "GG", "https://openapi.gg.go.kr/Dspsnwelfarefaclt", "Dspsnwelfarefaclt"),
    ETC_SOCIAL_SERVICE_PROVIDER(OpenApiCategory.LOCAL_WELFARE, "ETC", "https://api.socialservice.or.kr:444/api/service/provider/providerList", null),

    // =========================================================================
    // 4. EMPLOYMENT 계열 (총 5개)
    // =========================================================================
    // [소분류: REALTIME_JOB (id = 19)]
    ODCLOUD_KEAD_JOB_RECRUIT(OpenApiCategory.REALTIME_JOB, "ODCLOUD", "https://api.odcloud.kr/api/3072637/v1/uddi:6a9589d7-db1b-475b-b049-a957e834ed99", null),
    DATAGO_KEAD_REALTIME_RECRUIT(OpenApiCategory.REALTIME_JOB, "DATAGO", "https://apis.data.go.kr/B552583/job/job_list", null),
    DATAGO_ASSISTANT_RECRUIT_INFO(OpenApiCategory.REALTIME_JOB, "DATAGO", "https://apis.data.go.kr/B552583/assistjob/assist_job", null),

    // [소분류: KEAD_JOB (id = 20)]
    DATAGO_ASSISTANT_PROVIDER_REALTIME(OpenApiCategory.KEAD_JOB, "DATAGO", "https://apis.data.go.kr/B552583/instn/instn_list", null),

    // [소분류: STANDARD_WORKPLACE (id = 21)]
    ODCLOUD_KEAD_STANDARD_WORKPLACE(OpenApiCategory.STANDARD_WORKPLACE, "ODCLOUD", "https://api.odcloud.kr/api/3033670/v1/uddi:fe5b463e-0cc9-45a7-9c8d-ba96f76c1c2f", null),

    // =========================================================================
    // 5. EDUCATION 계열 (총 7개)
    // =========================================================================
    // [소분류: SPECIAL_SCHOOL (id = 15)]
    GG_SPECIAL_SCHOOL_STATUS_INFO(OpenApiCategory.SPECIAL_SCHOOL, "GG", "https://openapi.gg.go.kr/Ggspeclschoolstus", "Ggspeclschoolstus"),
    GG_SPECIAL_SCHOOL_MAJOR_DEPT(OpenApiCategory.SPECIAL_SCHOOL, "GG", "https://openapi.gg.go.kr/Ggmajkwaspeclstus", "Ggmajkwaspeclstus"),

    // [소분류: SPECIAL_EDU_SUPPORT (id = 16)]
    ODCLOUD_NISE_SPECIAL_EDU_CENTER(OpenApiCategory.SPECIAL_EDU_SUPPORT, "ODCLOUD", "https://api.odcloud.kr/api/15052681/v1/uddi:65f3aac9-9976-429e-ac86-94e93a06ddcb", null),
    ODCLOUD_NISE_SPECIAL_SCHOOL_STATUS(OpenApiCategory.SPECIAL_EDU_SUPPORT, "ODCLOUD", "https://api.odcloud.kr/api/15052682/v1/uddi:80cefa55-5b53-4bdb-9ef3-998945a82761", null),
    ODCLOUD_NISE_BASE_SCHOOL_STATUS(OpenApiCategory.SPECIAL_EDU_SUPPORT, "ODCLOUD", "https://api.odcloud.kr/api/15047505/v1/uddi:223ed298-82ad-4ef9-8a92-eb49bc9db782", null),
    ODCLOUD_NISE_SPECIAL_EDU_RESEARCH(OpenApiCategory.SPECIAL_EDU_SUPPORT, "ODCLOUD", "https://api.odcloud.kr/api/3059516/v1/uddi:52c807e9-64c2-4d0a-88c6-9cf929c98ec2", null),

    // [소분류: LIFELONG_EDU (id = 17)]
    ODCLOUD_NISE_LIFELONG_EDU_ORG(OpenApiCategory.LIFELONG_EDU, "ODCLOUD", "https://api.odcloud.kr/api/3057850/v1/uddi:7172e46b-e626-4c86-b767-ef1233f4d345", null);

    private final OpenApiCategory category;
    private final String urlType;
    private final String baseUrl;
    private final String apiKeyName; // 경기데이터드림 키(최상위)

    OpenApiSourceSpec(OpenApiCategory category, String urlType, String baseUrl, String apiKeyName) {
        this.category = category;
        this.urlType = urlType;
        this.baseUrl = baseUrl;
        this.apiKeyName = apiKeyName;
    }

    public static List<OpenApiSourceSpec> findByCategoryId(Long categoryId) {
        return Stream.of(values())
                .filter(spec -> spec.getCategory().getId().equals(categoryId))
                .toList();
    }
}