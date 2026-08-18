package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.entity.EmailAuthEntity;
import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.repository.EmailAuthRepository;
import kopo.poly.globalray.repository.UserBookmarkRepository;
import kopo.poly.globalray.repository.UserInfoRepository;
import kopo.poly.globalray.service.IEmailService;
import kopo.poly.globalray.service.IUserInfoService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements IUserInfoService {

    private final UserInfoRepository userInfoRepository;
    private final UserBookmarkRepository userBookmarkRepository;
    private final EmailAuthRepository emailAuthRepository;
    private final IEmailService emailService;

    // [추가] SecurityConfig에서 정의한 SHA256 PasswordEncoder Bean 주입
    //        비밀번호 암호화/비교를 SecurityConfig와 동일한 로직으로 통일
    private final PasswordEncoder passwordEncoder;

    /**
     * 읽기 전용 메서드에 @Transactional(readOnly = true) 를 추가하는 이유
     * - JPA 는 트랜잭션 내에서 엔티티 변경 감지(Dirty Checking)를 위해 스냅샷을 복사해둠
     * - readOnly = true 로 설정하면 스냅샷 복사와 변경 감지를 생략 → 성능 향상
     * - DB 드라이버/커넥션 풀 레벨에서 읽기 전용 최적화 힌트도 전달됨
     */

    @Override
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(String userId) {
        return userInfoRepository.existsByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailDuplicate(String email) {
        return userInfoRepository.existsByUserEmail(email);
    }

    @Override
    @Transactional
    public void sendEmailAuthCode(String email) throws Exception {
        emailAuthRepository.deleteByReqEmail(email);

        // SecureRandom: 암호학적으로 안전한 난수 생성기 (java.util.Random은 예측 가능하여 보안 코드에 부적합)
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        EmailAuthEntity auth = EmailAuthEntity.builder()
                .reqEmail(email)
                .authCode(EncryptUtil.encHashSHA256(code))
                .isVerified(0)
                .expireDt(LocalDateTime.now().plusMinutes(5))
                .build();
        emailAuthRepository.save(auth);

        emailService.sendAuthCode(email, code);
        log.info("인증코드 발송 완료 : {}", email);
    }

    // verifyEmailCode 는 인증 성공 시 isVerified 를 1로 수정하고 save() 하므로 readOnly = false (기본값)
    @Override
    @Transactional
    public boolean verifyEmailCode(String email, String code) {
        try {
            EmailAuthEntity auth = emailAuthRepository
                    .findTopByReqEmailOrderByAuthIdDesc(email).orElse(null);
            if (auth == null) return false;
            if (auth.getExpireDt().isBefore(LocalDateTime.now())) return false;
            if (!auth.getAuthCode().equals(EncryptUtil.encHashSHA256(code))) return false;

            auth.markVerified();
            emailAuthRepository.save(auth);
            return true;
        } catch (Exception e) {
            log.error("인증코드 검증 오류 : {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void signup(UserInfoDto dto) throws Exception {
        if (userInfoRepository.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userInfoRepository.existsByUserEmail(dto.getUserEmail())) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        // 이메일 인증 완료 여부 확인
        // - 프론트에서 인증 완료 여부를 관리하더라도 서버 측에서 반드시 재검증해야 함
        // - 그렇지 않으면 API 직접 호출로 인증 단계를 우회한 가입이 가능
        EmailAuthEntity auth = emailAuthRepository
                .findTopByReqEmailOrderByAuthIdDesc(dto.getUserEmail()).orElse(null);
        if (auth == null || auth.getIsVerified() != 1) {
            throw new IllegalArgumentException("이메일 인증이 완료되지 않았습니다.");
        }

        UserInfoEntity user = UserInfoEntity.builder()
                .userId(dto.getUserId())
                // [수정] EncryptUtil.encHashSHA256() 직접 호출 -> passwordEncoder.encode()로 교체
                //        (SecurityConfig의 PasswordEncoder Bean과 동일 로직이므로 결과값은 동일)
                .userPw(passwordEncoder.encode(dto.getUserPw()))
                .userName(dto.getUserName())
                .userEmail(dto.getUserEmail())
                .build();
        userInfoRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public String findUserId(String userName, String email) {
        return userInfoRepository.findByUserEmailAndUserName(email, userName)
                .map(UserInfoEntity::getUserId).orElse(null);
    }

    @Override
    @Transactional
    public boolean resetPassword(String userId, String email) throws Exception {
        Optional<UserInfoEntity> userOpt = userInfoRepository.findById(userId);
        if (userOpt.isEmpty()) return false;

        UserInfoEntity user = userOpt.get();
        if (!user.getUserEmail().equals(email)) return false;

        String tempPw = generateTempPassword();

        // [수정] EncryptUtil.encHashSHA256() 직접 호출 -> passwordEncoder.encode()로 교체
        user.changePassword(passwordEncoder.encode(tempPw));
        userInfoRepository.save(user);

        emailService.sendAuthCode(email, "임시 비밀번호: " + tempPw);
        log.info("임시 비밀번호 발급 완료 : {}", email);
        return true;
    }

    @Override
    @Transactional
    public boolean changePassword(String userId, String currentPw, String newPw) throws Exception {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // [수정] user.getUserPw().equals(EncryptUtil.encHashSHA256(currentPw))
        //        -> passwordEncoder.matches(currentPw, user.getUserPw())로 교체
        //        (matches 내부에서 동일하게 SHA256 암호화 후 비교)
        if (!passwordEncoder.matches(currentPw, user.getUserPw())) return false;

        // [수정] EncryptUtil.encHashSHA256() 직접 호출 -> passwordEncoder.encode()로 교체
        user.changePassword(passwordEncoder.encode(newPw));
        userInfoRepository.save(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoDto getUserInfo(String userId) {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return UserInfoDto.builder()
                .userId(user.getUserId())
                .userName(user.getUserName())
                .userEmail(user.getUserEmail())
                .socialType(CmmUtil.nvl(user.getSocialType()))
                .regDt(user.getRegDt())
                .build();
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        userBookmarkRepository.deleteByUserId(userId);
        userInfoRepository.deleteById(userId);
    }

    // 임시 비밀번호 생성 (SecureRandom 사용)
    private String generateTempPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        // SecureRandom: 예측 불가능한 난수로 임시 비밀번호 생성 (java.util.Random은 시드 예측 가능)
        SecureRandom rand = new SecureRandom();
        for (int i = 0; i < 10; i++) {
            sb.append(chars.charAt(rand.nextInt(chars.length())));
        }
        return sb.toString();
    }
}