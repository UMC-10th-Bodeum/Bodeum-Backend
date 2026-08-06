package com.bodeum.domain.info.service.publicDataService;

import com.bodeum.domain.info.dto.response.publicData.SyncDetailResultDto;
import com.bodeum.domain.info.dto.response.publicData.SyncResultSummaryDto;
import com.bodeum.domain.info.entity.enums.MainCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicDataSyncFacadeService {

    private final DisabilityJobSyncService disabilityJobSyncService;
    private final DisabledWelfareCenterSyncService disabledWelfareCenterSyncService;
    private final EmergencyMsgSyncService emergencyMsgSyncService;
    private final FamilySupportSyncService familySupportSyncService;
    private final LifelongEduCenterSyncService lifelongEduCenterSyncService;
    private final LocalWelfareSyncService localWelfareSyncService;
    private final NationalWelfareSyncService nationalWelfareSyncService;
    private final PrivateWelfareSyncService privateWelfareSyncService;
    private final PublicDoctorSyncService publicDoctorSyncService;
    private final SpecialEduCenterSyncService specialEduCenterSyncService;
    private final SpecialSchoolSyncService specialSchoolSyncService;
    private final StandardWorkplaceSyncService standardWorkplaceSyncService;
    private final TherapyRehabSyncService therapyRehabSyncService;

    /**
     * 1. 전체 공공데이터 비동기 백그라운드 순차 동기화
     */
    @Async("syncTaskExecutor")
    public void syncAllAsync() {
        log.info("[공공데이터 통합 동기화] 백그라운드 배치 작업 시작 - 실행 스레드: {}", Thread.currentThread().getName());
        syncAll(); // 내부 순차 실행 호출
        log.info("[공공데이터 통합 동기화] 백그라운드 배치 작업 전체 완료");
    }

    /**
     * 동기식 통합 동기화 (결과 리턴용)
     */
    public SyncResultSummaryDto syncAll() {
        List<SyncDetailResultDto> results = new ArrayList<>();

        // 하나씩 순차 실행되지만, 중간에 에러가 나도 catch하여 다음 작업으로 안전하게 진행
        results.add(executeIsolation(3L, MainCategory.HOSPITAL, "장애인 건강주치의", publicDoctorSyncService::syncPublicDoctorData));
        results.add(executeIsolation(4L, MainCategory.HOSPITAL, "응급실 메시지", emergencyMsgSyncService::syncEmergencyMessages));

        results.add(executeIsolation(6L, MainCategory.INSTITUTION, "치료·재활 기관", therapyRehabSyncService::syncTherapyRehabData));
        results.add(executeIsolation(7L, MainCategory.INSTITUTION, "장애인 복지관", disabledWelfareCenterSyncService::syncDisabledWelfareCenterData));
        results.add(executeIsolation(9L, MainCategory.INSTITUTION, "장애인가족지원센터", familySupportSyncService::syncFamilySupportData));

        results.add(executeIsolation(11L, MainCategory.WELFARE, "민간 복지 서비스", privateWelfareSyncService::syncPrivateWelfareData));
        results.add(executeIsolation(12L, MainCategory.WELFARE, "국가 복지 서비스", nationalWelfareSyncService::syncNationalWelfareData));
        results.add(executeIsolation(13L, MainCategory.WELFARE, "지역 복지 서비스", localWelfareSyncService::syncLocalWelfareData));

        results.add(executeIsolation(15L, MainCategory.EDUCATION, "특수학교 현황", specialSchoolSyncService::syncSpecialSchoolData));
        results.add(executeIsolation(16L, MainCategory.EDUCATION, "특수교육지원센터", specialEduCenterSyncService::syncSpecialEduCenterData));
        results.add(executeIsolation(17L, MainCategory.EDUCATION, "장애인 평생교육기관", lifelongEduCenterSyncService::syncLifelongEduCenterData));

        results.add(executeIsolation(19L, MainCategory.EMPLOYMENT, "실시간 구인 정보", disabilityJobSyncService::syncDisabilityJobData));
        results.add(executeIsolation(21L, MainCategory.EMPLOYMENT, "장애인 표준사업장", standardWorkplaceSyncService::syncStandardWorkplaceData));

        return SyncResultSummaryDto.of(results);
    }

    private SyncDetailResultDto executeIsolation(Long categoryId, MainCategory mainCategory, String serviceName, Runnable syncTask) {
        try {
            log.info(">>>> [{}] 동기화 시작 (Category ID: {})", serviceName, categoryId);
            syncTask.run();
            log.info(">>>> [{}] 동기화 성공 완료 (Category ID: {})", serviceName, categoryId);
            return SyncDetailResultDto.success(categoryId, mainCategory, serviceName, 0, 0);
        } catch (Exception e) {
            log.error(">>>> [{}] 동기화 실패했으나 다음 작업 계속 진행 (Category ID: {}): {}", serviceName, categoryId, e.getMessage());
            return SyncDetailResultDto.fail(categoryId, mainCategory, serviceName, e.getMessage());
        }
    }

    /**
     * 2. 대분류 카테고리별 비동기 동기화
     */
    @Async("syncTaskExecutor")
    public void syncByMainCategoryAsync(MainCategory mainCategory) {
        log.info("[공공데이터 대분류 동기화] 백그라운드 작업 시작 - 대상: {}", mainCategory);
        switch (mainCategory) {
            case HOSPITAL:
                executeIsolation(3L, MainCategory.HOSPITAL, "장애인 건강주치의", publicDoctorSyncService::syncPublicDoctorData);
                executeIsolation(4L, MainCategory.HOSPITAL, "응급실 메시지", emergencyMsgSyncService::syncEmergencyMessages);
                break;
            case INSTITUTION:
                executeIsolation(6L, MainCategory.INSTITUTION, "치료·재활 기관", therapyRehabSyncService::syncTherapyRehabData);
                executeIsolation(7L, MainCategory.INSTITUTION, "장애인 복지관", disabledWelfareCenterSyncService::syncDisabledWelfareCenterData);
                executeIsolation(9L, MainCategory.INSTITUTION, "장애인가족지원센터", familySupportSyncService::syncFamilySupportData);
                break;
            case WELFARE:
                executeIsolation(11L, MainCategory.WELFARE, "민간 복지 서비스", privateWelfareSyncService::syncPrivateWelfareData);
                executeIsolation(12L, MainCategory.WELFARE, "국가 복지 서비스", nationalWelfareSyncService::syncNationalWelfareData);
                executeIsolation(13L, MainCategory.WELFARE, "지역 복지 서비스", localWelfareSyncService::syncLocalWelfareData);
                break;
            case EDUCATION:
                executeIsolation(15L, MainCategory.EDUCATION, "특수학교 현황", specialSchoolSyncService::syncSpecialSchoolData);
                executeIsolation(16L, MainCategory.EDUCATION, "특수교육지원센터", specialEduCenterSyncService::syncSpecialEduCenterData);
                executeIsolation(17L, MainCategory.EDUCATION, "장애인 평생교육기관", lifelongEduCenterSyncService::syncLifelongEduCenterData);
                break;
            case EMPLOYMENT:
                executeIsolation(19L, MainCategory.EMPLOYMENT, "실시간 구인 정보", disabilityJobSyncService::syncDisabilityJobData);
                executeIsolation(21L, MainCategory.EMPLOYMENT, "장애인 표준사업장", standardWorkplaceSyncService::syncStandardWorkplaceData);
                break;
        }
    }

    /**
     * 3. 카테고리 ID 단건 비동기 동기화
     */
    @Async("syncTaskExecutor")
    public void syncByCategoryIdAsync(Long categoryId) {
        log.info("[공공데이터 카테고리 ID 단건 동기화] 백그라운드 작업 시작 - 대상 ID: {}", categoryId);
        if (categoryId == 3L) executeIsolation(3L, MainCategory.HOSPITAL, "장애인 건강주치의", publicDoctorSyncService::syncPublicDoctorData);
        else if (categoryId == 4L) executeIsolation(4L, MainCategory.HOSPITAL, "응급실 메시지", emergencyMsgSyncService::syncEmergencyMessages);
        else if (categoryId == 6L) executeIsolation(6L, MainCategory.INSTITUTION, "치료·재활 기관", therapyRehabSyncService::syncTherapyRehabData);
        else if (categoryId == 7L) executeIsolation(7L, MainCategory.INSTITUTION, "장애인 복지관", disabledWelfareCenterSyncService::syncDisabledWelfareCenterData);
        else if (categoryId == 9L) executeIsolation(9L, MainCategory.INSTITUTION, "장애인가족지원센터", familySupportSyncService::syncFamilySupportData);
        else if (categoryId == 11L) executeIsolation(11L, MainCategory.WELFARE, "민간 복지 서비스", privateWelfareSyncService::syncPrivateWelfareData);
        else if (categoryId == 12L) executeIsolation(12L, MainCategory.WELFARE, "국가 복지 서비스", nationalWelfareSyncService::syncNationalWelfareData);
        else if (categoryId == 13L) executeIsolation(13L, MainCategory.WELFARE, "지역 복지 서비스", localWelfareSyncService::syncLocalWelfareData);
        else if (categoryId == 15L) executeIsolation(15L, MainCategory.EDUCATION, "특수학교 현황", specialSchoolSyncService::syncSpecialSchoolData);
        else if (categoryId == 16L) executeIsolation(16L, MainCategory.EDUCATION, "특수교육지원센터", specialEduCenterSyncService::syncSpecialEduCenterData);
        else if (categoryId == 17L) executeIsolation(17L, MainCategory.EDUCATION, "장애인 평생교육기관", lifelongEduCenterSyncService::syncLifelongEduCenterData);
        else if (categoryId == 19L) executeIsolation(19L, MainCategory.EMPLOYMENT, "실시간 구인 정보", disabilityJobSyncService::syncDisabilityJobData);
        else if (categoryId == 21L) executeIsolation(21L, MainCategory.EMPLOYMENT, "장애인 표준사업장", standardWorkplaceSyncService::syncStandardWorkplaceData);
    }
}