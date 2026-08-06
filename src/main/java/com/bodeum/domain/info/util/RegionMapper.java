package com.bodeum.domain.info.util;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RegionMapper {

    private static final Map<String, Long> REGION_MAP = new HashMap<>();

    static {
        // === 서울특별시 (1 ~ 25) ===
        REGION_MAP.put("서울특별시 종로구", 1L);
        REGION_MAP.put("서울특별시 중구", 2L);
        REGION_MAP.put("서울특별시 용산구", 3L);
        REGION_MAP.put("서울특별시 성동구", 4L);
        REGION_MAP.put("서울특별시 광진구", 5L);
        REGION_MAP.put("서울특별시 동대문구", 6L);
        REGION_MAP.put("서울특별시 중랑구", 7L);
        REGION_MAP.put("서울특별시 성북구", 8L);
        REGION_MAP.put("서울특별시 강북구", 9L);
        REGION_MAP.put("서울특별시 도봉구", 10L);
        REGION_MAP.put("서울특별시 노원구", 11L);
        REGION_MAP.put("서울특별시 은평구", 12L);
        REGION_MAP.put("서울특별시 서대문구", 13L);
        REGION_MAP.put("서울특별시 마포구", 14L);
        REGION_MAP.put("서울특별시 양천구", 15L);
        REGION_MAP.put("서울특별시 강서구", 16L);
        REGION_MAP.put("서울특별시 구로구", 17L);
        REGION_MAP.put("서울특별시 금천구", 18L);
        REGION_MAP.put("서울특별시 영등포구", 19L);
        REGION_MAP.put("서울특별시 동작구", 20L);
        REGION_MAP.put("서울특별시 관악구", 21L);
        REGION_MAP.put("서울특별시 서초구", 22L);
        REGION_MAP.put("서울특별시 강남구", 23L);
        REGION_MAP.put("서울특별시 송파구", 24L);
        REGION_MAP.put("서울특별시 강동구", 25L);

        // === 전남광주통합특별시 (26 ~ 52) ===
        REGION_MAP.put("전남광주통합특별시 목포시", 26L);
        REGION_MAP.put("전남광주통합특별시 여수시", 27L);
        REGION_MAP.put("전남광주통합특별시 순천시", 28L);
        REGION_MAP.put("전남광주통합특별시 나주시", 29L);
        REGION_MAP.put("전남광주통합특별시 광양시", 30L);
        REGION_MAP.put("전남광주통합특별시 동구", 31L);
        REGION_MAP.put("전남광주통합특별시 서구", 32L);
        REGION_MAP.put("전남광주통합특별시 남구", 33L);
        REGION_MAP.put("전남광주통합특별시 북구", 34L);
        REGION_MAP.put("전남광주통합특별시 광산구", 35L);
        REGION_MAP.put("전남광주통합특별시 담양군", 36L);
        REGION_MAP.put("전남광주통합특별시 곡성군", 37L);
        REGION_MAP.put("전남광주통합특별시 구례군", 38L);
        REGION_MAP.put("전남광주통합특별시 고흥군", 39L);
        REGION_MAP.put("전남광주통합특별시 보성군", 40L);
        REGION_MAP.put("전남광주통합특별시 화순군", 41L);
        REGION_MAP.put("전남광주통합특별시 장흥군", 42L);
        REGION_MAP.put("전남광주통합특별시 강진군", 43L);
        REGION_MAP.put("전남광주통합특별시 해남군", 44L);
        REGION_MAP.put("전남광주통합특별시 영암군", 45L);
        REGION_MAP.put("전남광주통합특별시 무안군", 46L);
        REGION_MAP.put("전남광주통합특별시 함평군", 47L);
        REGION_MAP.put("전남광주통합특별시 영광군", 48L);
        REGION_MAP.put("전남광주통합특별시 장성군", 49L);
        REGION_MAP.put("전남광주통합특별시 완도군", 50L);
        REGION_MAP.put("전남광주통합특별시 진도군", 51L);
        REGION_MAP.put("전남광주통합특별시 신안군", 52L);

        // === 부산광역시 (53 ~ 68) ===
        REGION_MAP.put("부산광역시 중구", 53L);
        REGION_MAP.put("부산광역시 서구", 54L);
        REGION_MAP.put("부산광역시 동구", 55L);
        REGION_MAP.put("부산광역시 영도구", 56L);
        REGION_MAP.put("부산광역시 부산진구", 57L);
        REGION_MAP.put("부산광역시 동래구", 58L);
        REGION_MAP.put("부산광역시 남구", 59L);
        REGION_MAP.put("부산광역시 북구", 60L);
        REGION_MAP.put("부산광역시 해운대구", 61L);
        REGION_MAP.put("부산광역시 사하구", 62L);
        REGION_MAP.put("부산광역시 금정구", 63L);
        REGION_MAP.put("부산광역시 강서구", 64L);
        REGION_MAP.put("부산광역시 연제구", 65L);
        REGION_MAP.put("부산광역시 수영구", 66L);
        REGION_MAP.put("부산광역시 사상구", 67L);
        REGION_MAP.put("부산광역시 기장군", 68L);

        // === 대구광역시 (69 ~ 77) ===
        REGION_MAP.put("대구광역시 중구", 69L);
        REGION_MAP.put("대구광역시 동구", 70L);
        REGION_MAP.put("대구광역시 서구", 71L);
        REGION_MAP.put("대구광역시 남구", 72L);
        REGION_MAP.put("대구광역시 북구", 73L);
        REGION_MAP.put("대구광역시 수성구", 74L);
        REGION_MAP.put("대구광역시 달서구", 75L);
        REGION_MAP.put("대구광역시 달성군", 76L);
        REGION_MAP.put("대구광역시 군위군", 77L);

        // === 인천광역시 (78 ~ 88) ===
        REGION_MAP.put("인천광역시 제물포구", 78L);
        REGION_MAP.put("인천광역시 영종구", 79L);
        REGION_MAP.put("인천광역시 미추홀구", 80L);
        REGION_MAP.put("인천광역시 연수구", 81L);
        REGION_MAP.put("인천광역시 남동구", 82L);
        REGION_MAP.put("인천광역시 부평구", 83L);
        REGION_MAP.put("인천광역시 계양구", 84L);
        REGION_MAP.put("인천광역시 서해구", 85L);
        REGION_MAP.put("인천광역시 검단구", 86L);
        REGION_MAP.put("인천광역시 강화군", 87L);
        REGION_MAP.put("인천광역시 옹진군", 88L);

        // === 대전광역시 (89 ~ 93) ===
        REGION_MAP.put("대전광역시 동구", 89L);
        REGION_MAP.put("대전광역시 중구", 90L);
        REGION_MAP.put("대전광역시 서구", 91L);
        REGION_MAP.put("대전광역시 유성구", 92L);
        REGION_MAP.put("대전광역시 대덕구", 93L);

        // === 울산광역시 (94 ~ 98) ===
        REGION_MAP.put("울산광역시 중구", 94L);
        REGION_MAP.put("울산광역시 남구", 95L);
        REGION_MAP.put("울산광역시 동구", 96L);
        REGION_MAP.put("울산광역시 북구", 97L);
        REGION_MAP.put("울산광역시 울주군", 98L);

        // === 경기도 (99 ~ 129) ===
        REGION_MAP.put("경기도 수원시", 99L);
        REGION_MAP.put("경기도 성남시", 100L);
        REGION_MAP.put("경기도 의정부시", 101L);
        REGION_MAP.put("경기도 안양시", 102L);
        REGION_MAP.put("경기도 부천시", 103L);
        REGION_MAP.put("경기도 광명시", 104L);
        REGION_MAP.put("경기도 평택시", 105L);
        REGION_MAP.put("경기도 동두천시", 106L);
        REGION_MAP.put("경기도 안산시", 107L);
        REGION_MAP.put("경기도 고양시", 108L);
        REGION_MAP.put("경기도 과천시", 109L);
        REGION_MAP.put("경기도 구리시", 110L);
        REGION_MAP.put("경기도 남양주시", 111L);
        REGION_MAP.put("경기도 오산시", 112L);
        REGION_MAP.put("경기도 시흥시", 113L);
        REGION_MAP.put("경기도 군포시", 114L);
        REGION_MAP.put("경기도 의왕시", 115L);
        REGION_MAP.put("경기도 하남시", 116L);
        REGION_MAP.put("경기도 용인시", 117L);
        REGION_MAP.put("경기도 파주시", 118L);
        REGION_MAP.put("경기도 이천시", 119L);
        REGION_MAP.put("경기도 안성시", 120L);
        REGION_MAP.put("경기도 김포시", 121L);
        REGION_MAP.put("경기도 화성시", 122L);
        REGION_MAP.put("경기도 광주시", 123L);
        REGION_MAP.put("경기도 양주시", 124L);
        REGION_MAP.put("경기도 포천시", 125L);
        REGION_MAP.put("경기도 여주시", 126L);
        REGION_MAP.put("경기도 연천군", 127L);
        REGION_MAP.put("경기도 가평군", 128L);
        REGION_MAP.put("경기도 양평군", 129L);

        // === 충청북도 (130 ~ 140) ===
        REGION_MAP.put("충청북도 청주시", 130L);
        REGION_MAP.put("충청북도 충주시", 131L);
        REGION_MAP.put("충청북도 제천시", 132L);
        REGION_MAP.put("충청북도 보은군", 133L);
        REGION_MAP.put("충청북도 옥천군", 134L);
        REGION_MAP.put("충청북도 영동군", 135L);
        REGION_MAP.put("충청북도 증평군", 136L);
        REGION_MAP.put("충청북도 진천군", 137L);
        REGION_MAP.put("충청북도 괴산군", 138L);
        REGION_MAP.put("충청북도 음성군", 139L);
        REGION_MAP.put("충청북도 단양군", 140L);

        // === 충청남도 (141 ~ 155) ===
        REGION_MAP.put("충청남도 천안시", 141L);
        REGION_MAP.put("충청남도 공주시", 142L);
        REGION_MAP.put("충청남도 보령시", 143L);
        REGION_MAP.put("충청남도 아산시", 144L);
        REGION_MAP.put("충청남도 서산시", 145L);
        REGION_MAP.put("충청남도 논산시", 146L);
        REGION_MAP.put("충청남도 계룡시", 147L);
        REGION_MAP.put("충청남도 당진시", 148L);
        REGION_MAP.put("충청남도 금산군", 149L);
        REGION_MAP.put("충청남도 부여군", 150L);
        REGION_MAP.put("충청남도 서천군", 151L);
        REGION_MAP.put("충청남도 청양군", 152L);
        REGION_MAP.put("충청남도 홍성군", 153L);
        REGION_MAP.put("충청남도 예산군", 154L);
        REGION_MAP.put("충청남도 태안군", 155L);

        // === 경상북도 (156 ~ 177) ===
        REGION_MAP.put("경상북도 포항시", 156L);
        REGION_MAP.put("경상북도 경주시", 157L);
        REGION_MAP.put("경상북도 김천시", 158L);
        REGION_MAP.put("경상북도 안동시", 159L);
        REGION_MAP.put("경상북도 구미시", 160L);
        REGION_MAP.put("경상북도 영주시", 161L);
        REGION_MAP.put("경상북도 영천시", 162L);
        REGION_MAP.put("경상북도 상주시", 163L);
        REGION_MAP.put("경상북도 문경시", 164L);
        REGION_MAP.put("경상북도 경산시", 165L);
        REGION_MAP.put("경상북도 의성군", 166L);
        REGION_MAP.put("경상북도 청송군", 167L);
        REGION_MAP.put("경상북도 영양군", 168L);
        REGION_MAP.put("경상북도 영덕군", 169L);
        REGION_MAP.put("경상북도 청도군", 170L);
        REGION_MAP.put("경상북도 고령군", 171L);
        REGION_MAP.put("경상북도 성주군", 172L);
        REGION_MAP.put("경상북도 칠곡군", 173L);
        REGION_MAP.put("경상북도 예천군", 174L);
        REGION_MAP.put("경상북도 봉화군", 175L);
        REGION_MAP.put("경상북도 울진군", 176L);
        REGION_MAP.put("경상북도 울릉군", 177L);

        // === 경상남도 (178 ~ 195) ===
        REGION_MAP.put("경상남도 창원시", 178L);
        REGION_MAP.put("경상남도 진주시", 179L);
        REGION_MAP.put("경상남도 통영시", 180L);
        REGION_MAP.put("경상남도 사천시", 181L);
        REGION_MAP.put("경상남도 김해시", 182L);
        REGION_MAP.put("경상남도 밀양시", 183L);
        REGION_MAP.put("경상남도 거제시", 184L);
        REGION_MAP.put("경상남도 양산시", 185L);
        REGION_MAP.put("경상남도 의령군", 186L);
        REGION_MAP.put("경상남도 함안군", 187L);
        REGION_MAP.put("경상남도 창녕군", 188L);
        REGION_MAP.put("경상남도 고성군", 189L);
        REGION_MAP.put("경상남도 남해군", 190L);
        REGION_MAP.put("경상남도 하동군", 191L);
        REGION_MAP.put("경상남도 산청군", 192L);
        REGION_MAP.put("경상남도 함양군", 193L);
        REGION_MAP.put("경상남도 거창군", 194L);
        REGION_MAP.put("경상남도 합천군", 195L);

        // === 제주특별자치도 (196 ~ 197) ===
        REGION_MAP.put("제주특별자치도 제주시", 196L);
        REGION_MAP.put("제주특별자치도 서귀포시", 197L);

        // === 강원특별자치도 (198 ~ 215) ===
        REGION_MAP.put("강원특별자치도 춘천시", 198L);
        REGION_MAP.put("강원특별자치도 원주시", 199L);
        REGION_MAP.put("강원특별자치도 강릉시", 200L);
        REGION_MAP.put("강원특별자치도 동해시", 201L);
        REGION_MAP.put("강원특별자치도 태백시", 202L);
        REGION_MAP.put("강원특별자치도 속초시", 203L);
        REGION_MAP.put("강원특별자치도 삼척시", 204L);
        REGION_MAP.put("강원특별자치도 홍천군", 205L);
        REGION_MAP.put("강원특별자치도 횡성군", 206L);
        REGION_MAP.put("강원특별자치도 영월군", 207L);
        REGION_MAP.put("강원특별자치도 평창군", 208L);
        REGION_MAP.put("강원특별자치도 정선군", 209L);
        REGION_MAP.put("강원특별자치도 철원군", 210L);
        REGION_MAP.put("강원특별자치도 화천군", 211L);
        REGION_MAP.put("강원특별자치도 양구군", 212L);
        REGION_MAP.put("강원특별자치도 인제군", 213L);
        REGION_MAP.put("강원특별자치도 고성군", 214L);
        REGION_MAP.put("강원특별자치도 양양군", 215L);

        // === 전북특별자치도 (216 ~ 229) ===
        REGION_MAP.put("전북특별자치도 전주시", 216L);
        REGION_MAP.put("전북특별자치도 군산시", 217L);
        REGION_MAP.put("전북특별자치도 익산시", 218L);
        REGION_MAP.put("전북특별자치도 정읍시", 219L);
        REGION_MAP.put("전북특별자치도 남원시", 220L);
        REGION_MAP.put("전북특별자치도 김제시", 221L);
        REGION_MAP.put("전북특별자치도 완주군", 222L);
        REGION_MAP.put("전북특별자치도 진안군", 223L);
        REGION_MAP.put("전북특별자치도 무주군", 224L);
        REGION_MAP.put("전북특별자치도 장수군", 225L);
        REGION_MAP.put("전북특별자치도 임실군", 226L);
        REGION_MAP.put("전북특별자치도 순창군", 227L);
        REGION_MAP.put("전북특별자치도 고창군", 228L);
        REGION_MAP.put("전북특별자치도 부안군", 229L);

        // === 세종특별자치시 (230) ===
        REGION_MAP.put("세종특별자치시 세종특별자치시", 230L);
        REGION_MAP.put("세종특별자치시", 230L);
    }

    public Long resolveRegionId(String sido, String sigungu) {
        if (sido == null || sido.isBlank()) {
            return 230L;
        }

        String normalizedSido = normalizeSido(sido);
        String normalizedSigungu = extractMainSigungu(sigungu);

        String key = (normalizedSido + " " + normalizedSigungu).trim();
        Long regionId = REGION_MAP.get(key);

        if (regionId == null) {
            for (Map.Entry<String, Long> entry : REGION_MAP.entrySet()) {
                if (entry.getKey().startsWith(normalizedSido)) {
                    return entry.getValue();
                }
            }
            return 230L;
        }

        return regionId;
    }

    private String extractMainSigungu(String sigungu) {
        if (sigungu == null || sigungu.isBlank()) return "";
        String trimmed = sigungu.trim();
        String[] parts = trimmed.split("\\s+");
        return parts[0];
    }

    private String normalizeSido(String sido) {
        String trimmed = sido.trim();
        return switch (trimmed) {
            case "광주", "광주시", "광주광역시", "전남", "전라남도" -> "전남광주통합특별시";
            case "서울", "서울시" -> "서울특별시";
            case "부산", "부산시" -> "부산광역시";
            case "대구", "대구시" -> "대구광역시";
            case "인천", "인천시" -> "인천광역시";
            case "대전", "대전시" -> "대전광역시";
            case "울산", "울산시" -> "울산광역시";
            case "세종", "세종시" -> "세종특별자치시";
            case "경기" -> "경기도";
            case "강원", "강원도" -> "강원특별자치도";
            case "충북" -> "충청북도";
            case "충남" -> "충청남도";
            case "전북", "전북도" -> "전북특별자치도";
            case "경북" -> "경상북도";
            case "경남" -> "경상남도";
            case "제주", "제주시" -> "제주특별자치도";
            default -> trimmed;
        };
    }
}