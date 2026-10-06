package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.dto.LoginHistoryDto;
import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.dto.ViewHistoryDto;
import kopo.poly.globalray.entity.LoginHistoryEntity;
import kopo.poly.globalray.repository.LoginHistoryRepository;
import kopo.poly.globalray.repository.UserInfoRepository;
import kopo.poly.globalray.repository.ViewHistoryRepository;
import kopo.poly.globalray.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final UserInfoRepository userInfoRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    // Entity를 화면에 그대로 넘기면 비밀번호 해시까지 템플릿에 전달되므로 필요한 필드만 DTO로 변환
    @Override
    @Transactional(readOnly = true)
    public List<UserInfoDto> getAllUsers() {
        return userInfoRepository.findAll().stream()
                .map(u -> UserInfoDto.builder()
                        .userId(u.getUserId())
                        .userName(u.getUserName())
                        .userEmail(u.getUserEmail())
                        .socialType(u.getSocialType())
                        .regDt(u.getRegDt())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginHistoryDto> getRecentLoginHistory() {
        return loginHistoryRepository.findTop100ByOrderByLoginDtDesc().stream()
                .map(h -> LoginHistoryDto.builder()
                        .userId(h.getUserId())
                        .userName(h.getUserName())
                        .loginType(h.getLoginType())
                        .ipAddress(h.getIpAddress())
                        .loginDt(h.getLoginDt())
                        .build())
                .toList();
    }

    // 열람 이력은 MongoDB에 있어 JPA 트랜잭션 대상이 아님
    @Override
    public List<ViewHistoryDto> getRecentViewHistory() {
        return viewHistoryRepository.findTop100ByOrderByViewDtDesc().stream()
                .map(v -> ViewHistoryDto.builder()
                        .userId(v.getUserId())
                        .title(v.getTitle())
                        .viewDt(v.getViewDt())
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void saveLoginHistory(String userId, String userName, String ip, String loginType) {
        loginHistoryRepository.save(LoginHistoryEntity.builder()
                .userId(userId)
                .userName(userName)
                .ipAddress(ip)
                .loginType(loginType)
                .build());
        log.info("로그인 이력 저장: {} ({})", userId, loginType);
    }
}
