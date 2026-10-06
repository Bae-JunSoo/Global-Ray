package kopo.poly.globalray.service;

public interface IEmailService {
    void sendAuthCode(String toEmail, String authCode) throws Exception;

    void sendTempPassword(String toEmail, String tempPassword) throws Exception;
}
