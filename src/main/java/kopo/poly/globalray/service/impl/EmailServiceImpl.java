package kopo.poly.globalray.service.impl;

import jakarta.mail.internet.MimeMessage;
import kopo.poly.globalray.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements IEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendAuthCode(String toEmail, String authCode) throws Exception {
        send(toEmail, "[GlobalRay] 이메일 인증코드", """
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>GlobalRay 이메일 인증</h2>
                    <p>아래 인증코드를 입력해주세요. (5분간 유효)</p>
                    <h1 style="color: #111; letter-spacing: 8px;">%s</h1>
                </div>
                """.formatted(authCode));
    }

    @Override
    public void sendTempPassword(String toEmail, String tempPassword) throws Exception {
        send(toEmail, "[GlobalRay] 임시 비밀번호 안내", """
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2>GlobalRay 임시 비밀번호</h2>
                    <p>아래 임시 비밀번호로 로그인한 뒤, 마이페이지에서 비밀번호를 꼭 변경해주세요.</p>
                    <h2 style="color: #111;">%s</h2>
                </div>
                """.formatted(tempPassword));
    }

    private void send(String toEmail, String subject, String html) throws Exception {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("메일 발송 완료 [{}] : {}", subject, toEmail);
        } catch (Exception e) {
            log.error("메일 발송 실패 [{}] : {}", subject, toEmail, e);
            throw e;
        }
    }
}
