package kopo.poly.globalray.service;

import kopo.poly.globalray.entity.LoginHistoryEntity;
import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.entity.ViewHistoryEntity;

import java.util.List;

public interface IAdminService {
    List<UserInfoEntity> getAllUsers();
    List<LoginHistoryEntity> getRecentLoginHistory();
    List<ViewHistoryEntity> getRecentViewHistory();
    void saveLoginHistory(String userId, String userName, String ip, String loginType);
}
