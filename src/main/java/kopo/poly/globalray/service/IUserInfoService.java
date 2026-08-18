package kopo.poly.globalray.service;

import kopo.poly.globalray.dto.UserInfoDto;


public interface IUserInfoService {

    // 아이디 중복 확인
    boolean isUserIdDuplicate(String userId);

    // 이메일 중복 확인
    boolean isEmailDuplicate(String email);

    // 이메일 인증코드 발송
    void sendEmailAuthCode(String email) throws Exception;

    // 이메일 인증코드 검증
    boolean verifyEmailCode(String email, String code);

    // 회원가입
    void signup(UserInfoDto dto) throws Exception;

    // 아이디 찾기
    String findUserId(String userName, String email);

    // 비밀번호 재설정 (임시 비밀번호 발송)
    boolean resetPassword(String userId, String email) throws Exception;

    // 비밀번호 변경
    boolean changePassword(String userId, String currentPw, String newPw) throws Exception;

    // 회원 정보 조회
    UserInfoDto getUserInfo(String userId);

    // 회원 탈퇴
    void deleteUser(String userId);
}