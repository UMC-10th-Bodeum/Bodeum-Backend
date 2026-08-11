package com.bodeum.domain.news.entity;

import java.util.Map;

final class NewsExternalLinkResolver {

    private static final String SUNCHEON_WELFARE_CENTER = "순천시장애인종합복지관";
    private static final Map<String, String> SOURCE_URLS = Map.ofEntries(
            Map.entry(SUNCHEON_WELFARE_CENTER, "http://www.scrw.or.kr/"),
            Map.entry("구리시장애인종합복지관", "https://guriwel.or.kr/"),
            Map.entry("성남시장애인종합복지관", "https://www.rehab21.or.kr/"),
            Map.entry("부산광역시 금정구 장애인복지관", "https://www.gjrc.or.kr/"),
            Map.entry("사하구장애인종합복지관", "http://www.saharc.or.kr/"),
            Map.entry("진안군장애인종합복지관", "https://www.jinanrc.or.kr/"),
            Map.entry("영도구장애인복지관", "https://www.yeongdorc.or.kr/"),
            Map.entry("동구한마음종합복지관", "http://hanmaeum.org/"),
            Map.entry("합천군장애인복지센터", "https://www.assist.or.kr/"),
            Map.entry("사단법인 해솔", "http://haesols.modoo.at/"),
            Map.entry("광교아동발달센터", "http://xn--hc0bse75oc1af61a7vhkrhzwz.kr/"),
            Map.entry("꿈고래사회적협동조합", "https://dreamwhale.org/"),
            Map.entry("새길온사회적협동조합", "https://www.saegilon.co.kr/"),
            Map.entry("한국아동발달 사회적협동조합", "https://all-live.kr/"),
            Map.entry("해담심리언어발달센터", "http://www.haedamcenter.com/"),
            Map.entry("해담심리언어발달센터 2호점", "http://www.haedamcenter.com/"),
            Map.entry("아주청각언어센터", "https://hellosu26.wixsite.com/ajouhearing/blank-yur91"),
            Map.entry("우리아이통합발달센터", "https://uriai-center.com/"),
            Map.entry("우리아이통합발달센터 권선점", "https://uriai-center.com/")
    );

    private static final Map<LinkKey, String> PROGRAM_URLS = Map.ofEntries(
            entry(SUNCHEON_WELFARE_CENTER, "장애공감문화", 6, 125),
            entry(SUNCHEON_WELFARE_CENTER, "자연체험활동", 1, 34208),
            entry(SUNCHEON_WELFARE_CENTER, "동산골축제", 2, 34220),
            entry(SUNCHEON_WELFARE_CENTER, "내가그린에코", 2, 34283),
            entry(SUNCHEON_WELFARE_CENTER, "정보화교육", 1, 34327),
            entry(SUNCHEON_WELFARE_CENTER, "지원고용", 1, 34224),
            entry(SUNCHEON_WELFARE_CENTER, "근로지원인", 1, 34360),
            entry(SUNCHEON_WELFARE_CENTER, "장애인일자리사업", 2, 34360),
            entry(SUNCHEON_WELFARE_CENTER, "장애인즐거운한마당", 1, 33633),
            entry(SUNCHEON_WELFARE_CENTER, "자조활동", 1, 34176),
            entry(SUNCHEON_WELFARE_CENTER, "신비한 과학이야기", 2, 33737),
            entry(SUNCHEON_WELFARE_CENTER, "음악활동", 1, 33901),
            entry(SUNCHEON_WELFARE_CENTER, "여성장애인교육지원사업", 1, 34371),
            entry(SUNCHEON_WELFARE_CENTER, "생생사진관", 1, 34252),
            entry(SUNCHEON_WELFARE_CENTER, "한글배움터", 1, 34316),
            entry(SUNCHEON_WELFARE_CENTER, "생활체육", 1, 34317),
            entry(SUNCHEON_WELFARE_CENTER, "상상누림터", 1, 34337),
            entry(SUNCHEON_WELFARE_CENTER, "동산오락실", 1, 34323),
            entry(SUNCHEON_WELFARE_CENTER, "노리존", 1, 34184),
            entry(SUNCHEON_WELFARE_CENTER, "문화활동", 2, 33919),
            Map.entry(
                    new LinkKey("부산광역시 금정구 장애인복지관", "외부연계프로그램 금토피아"),
                    "https://www.gjrc.or.kr/SW_bbs/view.php?zipEncode=Zitm90wDU91DLLMDMqMBLrhDH91vt1drjrMCH9MyMetpSfMvWLME"
            )
    );

    private NewsExternalLinkResolver() {
    }

    static String resolve(String sourceName, String title) {
        if (sourceName == null || sourceName.isBlank()) {
            return null;
        }

        String programUrl = PROGRAM_URLS.get(new LinkKey(sourceName, title));
        return programUrl != null ? programUrl : SOURCE_URLS.get(sourceName);
    }

    private static Map.Entry<LinkKey, String> entry(
            String sourceName,
            String title,
            int boardCode,
            int articleNumber
    ) {
        return Map.entry(
                new LinkKey(sourceName, title),
                "http://www.scrw.or.kr/bbs/view.php?wcode="
                        + String.format("%02d", boardCode)
                        + "&wnum="
                        + articleNumber
        );
    }

    private record LinkKey(String sourceName, String title) {
    }
}
