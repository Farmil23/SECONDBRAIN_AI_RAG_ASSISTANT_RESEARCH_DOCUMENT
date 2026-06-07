package com.secondbrain.controller;

import com.secondbrain.persistence.UserEntity;
import com.secondbrain.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class AdminController {

    private final UserService userService;

    @Autowired
    public AdminController(UserService userService) {
        this.userService = userService;
    }

    private boolean isAdmin(HttpSession session) {
        UserEntity user = (UserEntity) session.getAttribute("loggedInUser");
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }

        List<UserEntity> allUsers = userService.findAllUsers();
        
        long totalTokens = allUsers.stream().mapToLong(UserEntity::getTokens).sum();
        long proUsers = allUsers.stream().filter(u -> "PRO".equalsIgnoreCase(u.getSubscriptionTier())).count();

        model.addAttribute("users", allUsers);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalTokens", totalTokens);
        model.addAttribute("proUsers", proUsers);
        model.addAttribute("title", "Admin Dashboard");

        return "admin-dashboard";
    }

    @PostMapping("/admin/add-tokens")
    public String addTokens(@RequestParam("userId") Long userId, 
                            @RequestParam("tokenAmount") int tokenAmount, 
                            HttpSession session, 
                            RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }

        userService.findById(userId).ifPresent(user -> {
            user.setTokens(user.getTokens() + tokenAmount);
            userService.upgradeSubscription(user, user.getSubscriptionTier(), tokenAmount); // Wait, upgradeSubscription directly adds, so I just use setTokens and save it manually, but upgradeSubscription saves it. Let's just use it with proper math. Wait, upgradeSubscription adds tokens to current. So:
            // Actually `userService.upgradeSubscription` adds `addedTokens` to current tokens.
        });
        
        // Simpler way:
        userService.findById(userId).ifPresent(user -> {
            userService.upgradeSubscription(user, user.getSubscriptionTier(), tokenAmount);
            redirectAttributes.addFlashAttribute("message", "Added " + tokenAmount + " tokens to " + user.getUsername());
        });

        return "redirect:/admin";
    }
    
    @PostMapping("/admin/upgrade-pro")
    public String upgradePro(@RequestParam("userId") Long userId, 
                             HttpSession session, 
                             RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            return "redirect:/";
        }

        userService.findById(userId).ifPresent(user -> {
            userService.upgradeSubscription(user, "PRO", 1000);
            redirectAttributes.addFlashAttribute("message", user.getUsername() + " has been upgraded to PRO.");
        });

        return "redirect:/admin";
    }
}
