package kopo.poly.globalray.service;

import kopo.poly.globalray.dto.LoginHistoryDto;
import kopo.poly.globalray.dto.UserInfoDto;
import kopo.poly.globalray.dto.ViewHistoryDto;

import java.util.List;

public interface IAdminService {
    List<UserInfoDto> getAllUsers();
    List<LoginHistoryDto> getRecentLoginHistory();
    List<ViewHistoryDto> getRecentViewHistory();
    void saveLoginHistory(String userId, String userName, String ip, String loginType);
}
