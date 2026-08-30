package kopo.poly.globalray.controller;

import kopo.poly.globalray.service.IAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final IAdminService adminService;

    @GetMapping({"", "/"})
    public String adminMain(Model model) {
        var users = adminService.getAllUsers();
        var loginHistory = adminService.getRecentLoginHistory();
        var viewHistory = adminService.getRecentViewHistory();

        model.addAttribute("users", users);
        model.addAttribute("loginHistory", loginHistory);
        model.addAttribute("viewHistory", viewHistory);
        model.addAttribute("userCount", users.size());
        model.addAttribute("loginCount", loginHistory.size());
        model.addAttribute("viewCount", viewHistory.size());
        return "admin/index";
    }
}
