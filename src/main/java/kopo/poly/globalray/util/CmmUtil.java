package kopo.poly.globalray.util;

public class CmmUtil {

    private CmmUtil() {}

    public static String nvl(String str, String chgStr) {
        if (str == null || str.isEmpty()) return chgStr;
        return str;
    }

    public static String nvl(String str) {
        return nvl(str, "");
    }

    // 본문 글자수 제한 + 제어문자 제거 (Gemini 토큰 절약용)
    public static String truncate(String content, int maxLength) {
        if (content == null || content.isBlank()) return "";
        String cleaned = content
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
    }
}
