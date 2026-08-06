package com.bodeum.domain.info.service;

import com.bodeum.domain.info.repository.InfoScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InfoScrapService {

    private final InfoScrapRepository infoScrapRepository;

    // 회원 탈퇴 시: 해당 회원의 정보 스크랩을 삭제하고 각 정보의 scrapCount를 감소시킨다.
    // 카운트 감소를 삭제보다 먼저 수행한다.
    @Transactional
    public void deleteUserScraps(Long userId) {
        infoScrapRepository.decreaseScrapCountForUserScraps(userId);
        infoScrapRepository.deleteByUserId(userId);
    }
}
