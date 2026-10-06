package kopo.poly.globalray.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

// 이메일 인증코드처럼 짧은 시간만 쓰는 값을 해시로 저장할 때 사용 (회원 비밀번호는 BCrypt 사용)
public class EncryptUtil {

    // 같은 코드라도 해시값을 예측하기 어렵게 앞에 붙이는 고정 문자열
    private static final String ADD_MESSAGE = "PolyDataAnalysis";

    private EncryptUtil() {}

    public static String encHashSHA256(String str) {
        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-256");
            byte[] byteData = sh.digest((ADD_MESSAGE + str).getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : byteData) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 모든 JVM이 반드시 지원해야 하는 알고리즘이라 실제로는 발생하지 않음
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
