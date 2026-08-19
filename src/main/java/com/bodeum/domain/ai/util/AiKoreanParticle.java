package com.bodeum.domain.ai.util;

/**
 * 마지막 한글 음절의 받침 여부에 따라 응답 문구에 사용할 조사를 선택한다.
 */
public final class AiKoreanParticle {

    private AiKoreanParticle() {
    }

    public static String object(String value) {
        return hasFinalConsonant(value) ? "을" : "를";
    }

    public static String topic(String value) {
        return hasFinalConsonant(value) ? "은" : "는";
    }

    public static String directional(String value) {
        char last = lastKoreanSyllable(value);
        if (last == 0) {
            return "로";
        }
        int finalConsonant = (last - 0xAC00) % 28;
        return finalConsonant == 0 || finalConsonant == 8 ? "로" : "으로";
    }

    private static boolean hasFinalConsonant(String value) {
        char last = lastKoreanSyllable(value);
        return last != 0 && (last - 0xAC00) % 28 != 0;
    }

    private static char lastKoreanSyllable(String value) {
        if (value == null) {
            return 0;
        }
        for (int index = value.length() - 1; index >= 0; index--) {
            char current = value.charAt(index);
            if (current >= 0xAC00 && current <= 0xD7A3) {
                return current;
            }
        }
        return 0;
    }
}
