package kopo.poly.globalray.controller;

import kopo.poly.globalray.entity.LoginHistoryEntity;
import kopo.poly.globalray.entity.UserInfoEntity;
import kopo.poly.globalray.entity.ViewHistoryEntity;
import kopo.poly.globalray.repository.LoginHistoryRepository;
import kopo.poly.globalray.repository.UserInfoRepository;
import kopo.poly.globalray.repository.ViewHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserInfoRepository userInfoRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    @GetMapping({"", "/"})
    public String adminMain(Model model) {
        List<UserInfoEntity> users = userInfoRepository.findAll();
        List<LoginHistoryEntity> loginHistory = loginHistoryRepository.findTop100ByOrderByLoginDtDesc();
        List<ViewHistoryEntity> viewHistory = viewHistoryRepository.findTop100ByOrderByViewDtDesc();

        model.addAttribute("users", users);
        model.addAttribute("loginHistory", loginHistory);
        model.addAttribute("viewHistory", viewHistory);
        model.addAttribute("userCount", users.size());
        model.addAttribute("loginCount", loginHistory.size());
        model.addAttribute("viewCount", viewHistory.size());
        return "admin/index";
    }
}
