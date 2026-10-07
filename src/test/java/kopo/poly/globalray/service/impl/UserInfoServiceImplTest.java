package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.EmailAuthEntity;
import kopo.poly.globalray.repository.EmailAuthRepository;
import kopo.poly.globalray.service.IEmailService;
import kopo.poly.globalray.util.EncryptUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// DB 없이 이메일 인증 규칙(시도 횟수 제한, 재발송 간격)만 검증하는 단위 테스트
@ExtendWith(MockitoExtension.class)
class UserInfoServiceImplTest {

    private static final String EMAIL = "test@example.com";
    private static final String CODE = "123456";

    @Mock private EmailAuthRepository emailAuthRepository;
    @Mock private IEmailService emailService;
    @InjectMocks private UserInfoServiceImpl userInfoService;

    private EmailAuthEntity authSentMinutesAgo(long minutesAgo, int failCount) {
        return EmailAuthEntity.builder()
                .reqEmail(EMAIL)
                .authCode(EncryptUtil.encHashSHA256(CODE))
                .isVerified(0)
                .expireDt(LocalDateTime.now().plusMinutes(5 - minutesAgo))
                .failCount(failCount)
                .build();
    }

    @Test
    void 틀린_코드를_입력하면_실패_횟수가_1_증가한다() {
        EmailAuthEntity auth = authSentMinutesAgo(1, 0);
        when(emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(EMAIL)).thenReturn(Optional.of(auth));

        assertFalse(userInfoService.verifyEmailCode(EMAIL, "000000"));
        assertEquals(1, auth.getFailCount());
        assertEquals(0, auth.getIsVerified());
    }

    @Test
    void 맞는_코드를_입력하면_인증이_완료된다() {
        EmailAuthEntity auth = authSentMinutesAgo(1, 2);
        when(emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(EMAIL)).thenReturn(Optional.of(auth));

        assertTrue(userInfoService.verifyEmailCode(EMAIL, CODE));
        assertEquals(1, auth.getIsVerified());
    }

    @Test
    void 다섯_번_틀린_뒤에는_맞는_코드도_거부된다() {
        EmailAuthEntity auth = authSentMinutesAgo(1, 5);
        when(emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(EMAIL)).thenReturn(Optional.of(auth));

        assertThrows(IllegalArgumentException.class, () -> userInfoService.verifyEmailCode(EMAIL, CODE));
        assertEquals(0, auth.getIsVerified());
    }

    @Test
    void 만료된_코드는_거부된다() {
        EmailAuthEntity auth = authSentMinutesAgo(6, 0);
        when(emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(EMAIL)).thenReturn(Optional.of(auth));

        assertFalse(userInfoService.verifyEmailCode(EMAIL, CODE));
    }

    @Test
    void 발송_후_60초_안에_다시_요청하면_거부되고_메일을_보내지_않는다() throws Exception {
        EmailAuthEntity last = authSentMinutesAgo(0, 0);
        when(emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(EMAIL)).thenReturn(Optional.of(last));

        assertThrows(IllegalArgumentException.class, () -> userInfoService.sendEmailAuthCode(EMAIL));
        verify(emailService, never()).sendAuthCode(anyString(), anyString());
        verify(emailAuthRepository, never()).deleteByReqEmail(anyString());
    }

    @Test
    void 발송_후_60초가_지나면_다시_보낼_수_있다() throws Exception {
        EmailAuthEntity last = authSentMinutesAgo(2, 0);
        when(emailAuthRepository.findTopByReqEmailOrderByAuthIdDesc(EMAIL)).thenReturn(Optional.of(last));

        userInfoService.sendEmailAuthCode(EMAIL);

        verify(emailAuthRepository).deleteByReqEmail(EMAIL);
        verify(emailService).sendAuthCode(eq(EMAIL), anyString());
    }
}
