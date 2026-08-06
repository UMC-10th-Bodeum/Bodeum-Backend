package com.bodeum.domain.info.service;

import com.bodeum.domain.info.repository.InfoRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoRegionQueryService {

    private final InfoRegionRepository infoRegionRepository;

    public List<String> getSidoList() {
        return infoRegionRepository.findDistinctSido();
    }

    public List<String> getSigunguList(String sido) {
        return infoRegionRepository.findSigunguBySido(sido);
    }
}