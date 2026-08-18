package kopo.poly.globalray.util;

public class CmmUtil {
	public static String nvl(String str, String chg_str) {
		String res;
		if (str == null) {
			res = chg_str;
		} else if (str.equals("")) {
			res = chg_str;
		} else {
			res = str;
		}
		return res;
	}

	public static String nvl(String str) {
		return nvl(str, "");
	}

	public static String checked(String str, String com_str) {
		if (str.equals(com_str)) {
			return " checked";
		} else {
			return "";
		}
	}

	public static String checked(String[] str, String com_str) {
		for (int i = 0; i < str.length; i++) {
			if (str[i].equals(com_str))
				return " checked";
		}
		return "";
	}

	public static String select(String str, String com_str) {
		if (str.equals(com_str)) {
			return " selected";
		} else {
			return "";
		}
	}

	// 본문 글자수 제한 + 특수문자 제거 (Gemini 토큰 절약용)
	// GeminiServiceImpl, NewsCollectServiceImpl 중복 제거
	public static String truncate(String content, int maxLength) {
		if (content == null || content.isBlank()) return "";
		String cleaned = content
				.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "")
				.replaceAll("\\s+", " ")
				.trim();
		return cleaned.length() > maxLength ? cleaned.substring(0, maxLength) : cleaned;
	}
}