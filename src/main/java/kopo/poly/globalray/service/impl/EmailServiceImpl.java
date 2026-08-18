package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Override
    public void sendAuthCode(String toEmail, String authCode) throws Exception {
        log.info("이메일 인증코드 발송 시작 : {}", toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("ssc9023@naver.com");
            helper.setTo(toEmail);
            helper.setSubject("[GlobalRay] 이메일 인증코드");
            helper.setText("""
                    <div style="font-family: Arial, sans-serif; padding: 20px;">
                        <h2>GlobalRay 이메일 인증</h2>
                        <p>아래 인증코드를 입력해주세요.</p>
                        <h1 style="color: #111; letter-spacing: 8px;">%s</h1>
                    </div>
                    """.formatted(authCode), true);

            mailSender.send(message);
            log.info("이메일 인증코드 발송 완료 : {}", toEmail);

        } catch (Exception e) {
            log.error("이메일 발송 실패 : {} - {}", toEmail, e.getMessage(), e);
            throw e;
        }
    }
}