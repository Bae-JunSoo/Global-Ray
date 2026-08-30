package kopo.poly.globalray.service.impl;

import kopo.poly.globalray.entity.LoginHistoryEntity;
import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.entity.ViewHistoryEntity;
import kopo.poly.globalray.repository.LoginHistoryRepository;
import kopo.poly.globalray.repository.UserInfoRepository;
import kopo.poly.globalray.repository.ViewHistoryRepository;
import kopo.poly.globalray.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements IAdminService {

    private final UserInfoRepository userInfoRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserInfoEntity> getAllUsers() {
        return userInfoRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoginHistoryEntity> getRecentLoginHistory() {
        return loginHistoryRepository.findTop100ByOrderByLoginDtDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViewHistoryEntity> getRecentViewHistory() {
        return viewHistoryRepository.findTop100ByOrderByViewDtDesc();
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
