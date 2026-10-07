package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.exception.NotFoundException;
import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.entity.EmailAuthEntity;
import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.repository.ChatHistoryRepository;
import kopo.poly.globalray.repository.EmailAuthRepository;
import kopo.poly.globalray.repository.UserBookmarkRepository;
import kopo.poly.globalray.repository.UserInfoRepository;
import kopo.poly.globalray.service.IEmailService;
import kopo.poly.globalray.service.ILikeService;
import kopo.poly.globalray.service.IUserInfoService;
import kopo.poly.globalray.util.CmmUtil;
import kopo.poly.globalray.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final int CODE_VALID_MINUTES = 5;
    private static final int RESEND_INTERVAL_SECONDS = 60;
    private static final int MAX_VERIFY_ATTEMPTS = 5;

    private final UserInfoRepository userInfoRepository;
    private final UserBookmarkRepository userBookmarkRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final EmailAuthRepository emailAuthRepository;
    private final IEmailService emailService;
    private final ILikeService likeService;
    // 로그인 시 비교에 쓰는 것과 같은 BCrypt 인코더 (PasswordEncoderConfig)
    private final PasswordEncoder passwordEncoder;

    // 조회 전용 메서드는 readOnly = true: 변경 감지용 스냅샷을 만들지 않아 불필요한 비용을 줄임
    @Override
    @Transactional(readOnly = true)
    public boolean isUserIdDuplicate(String userId) {
        return userInfoRepository.existsByUserId(userId);
    }

    @Override
    @Transactional
    public void sendEmailAuthCode(String email) throws Exception {
        // 같은 주소로 연달아 발송하면 메일 폭탄이 되고 SMTP 계정이 차단될 수 있으므로 간격을 둠
        emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(email).ifPresent(last -> {
            LocalDateTime sentAt = last.getExpireDt().minusMinutes(CODE_VALID_MINUTES);
            if (sentAt.plusSeconds(RESEND_INTERVAL_SECONDS).isAfter(LocalDateTime.now())) {
                throw new IllegalArgumentException("인증코드는 " + RESEND_INTERVAL_SECONDS + "초 후에 다시 요청할 수 있습니다.");
            }
        });

        emailAuthRepository.deleteByReqEmail(email);

        // SecureRandom: 암호학적으로 안전한 난수 생성기 (java.util.Random은 예측 가능하여 보안 코드에 부적합)
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        EmailAuthEntity auth = EmailAuthEntity.builder()
                .reqEmail(email)
                .authCode(EncryptUtil.encHashSHA256(code))
                .isVerified(0)
                .expireDt(LocalDateTime.now().plusMinutes(CODE_VALID_MINUTES))
                .build();
        emailAuthRepository.save(auth);

        emailService.sendAuthCode(email, code);
        log.info("인증코드 발송 완료 : {}", email);
    }

    // 6자리 코드는 경우의 수가 100만 개뿐이라 시도 횟수를 제한하지 않으면 자동 대입으로 뚫릴 수 있음
    // 실패 횟수는 변경 감지로 커밋 시 저장되므로 readOnly가 아닌 일반 트랜잭션
    @Override
    @Transactional
    public boolean verifyEmailCode(String email, String code) {
        EmailAuthEntity auth = emailAuthRepository
                .findTopByReqEmailOrderByAuthIdDesc(email).orElse(null);
        if (auth == null) return false;
        if (auth.getExpireDt().isBefore(LocalDateTime.now())) return false;
        if (auth.getFailCount() >= MAX_VERIFY_ATTEMPTS) {
            throw new IllegalArgumentException("인증 시도 횟수를 초과했습니다. 인증코드를 다시 요청해주세요.");
        }

        if (!auth.getAuthCode().equals(EncryptUtil.encHashSHA256(code))) {
            auth.increaseFailCount();
            return false;
        }

        auth.markVerified();
        return true;
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
        user.changePassword(passwordEncoder.encode(tempPw));
        userInfoRepository.save(user);

        emailService.sendTempPassword(email, tempPw);
        log.info("임시 비밀번호 발급 완료 : {}", email);
        return true;
    }

    @Override
    @Transactional
    public boolean changePassword(String userId, String currentPw, String newPw) throws Exception {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        // BCrypt는 매번 다른 salt를 쓰므로 equals로 비교할 수 없고 matches()로 비교해야 함
        if (!passwordEncoder.matches(currentPw, user.getUserPw())) return false;

        user.changePassword(passwordEncoder.encode(newPw));
        userInfoRepository.save(user);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoDto getUserInfo(String userId) {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

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
        chatHistoryRepository.deleteByUserId(userId);
        likeService.deleteAllByUser(userId);
        userInfoRepository.deleteById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoDto getUserInfoForAuth(String userId) {
        UserInfoEntity user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다 : " + userId));
        return UserInfoDto.builder()
                .userId(user.getUserId())
                .userPw(user.getUserPw())
                .userEmail(user.getUserEmail())
                .userName(user.getUserName())
                .build();
    }

    @Override
    @Transactional
    public UserInfoDto findOrCreateOAuth2User(String userId, String email, String name, String socialType) {
        UserInfoEntity user = userInfoRepository.findByUserEmail(email)
                .orElseGet(() -> {
                    UserInfoEntity newUser = UserInfoEntity.builder()
                            .userId(userId)
                            .userPw("")
                            .userName(name)
                            .userEmail(email)
                            .socialType(socialType)
                            .build();
                    log.info("신규 OAuth2 유저 등록 : {} ({})", email, socialType);
                    return userInfoRepository.save(newUser);
                });
        return UserInfoDto.builder()
                .userId(user.getUserId())
                .userEmail(user.getUserEmail())
                .userName(user.getUserName())
                .build();
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